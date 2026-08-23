package pro.api4.jsonapi4j.plugin.ac;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.context.DefaultAccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
public abstract class AccessControlEvaluator implements InboundAccessControlEvaluator, OutboundAccessControlEvaluator {

    public static <DATA> DATA retrieveDataIfAllowed(AccessControlEvaluator accessControlEvaluator,
                                                    AccessControlContext context,
                                                    Supplier<DATA> dataSupplier,
                                                    AccessControlModel inboundAccessControlSettings) {
        if (accessControlEvaluator != null) {
            // retrieve downstream data if allowed
            return accessControlEvaluator.retrieveDataIfAllowed(
                    context,
                    dataSupplier,
                    inboundAccessControlSettings
            );
        } else {
            return dataSupplier.get();
        }
    }

    public static <T> AnonymizationResult<T> anonymizeObjectIfNeeded(
            AccessControlEvaluator accessControlEvaluator,
            T targetObject,
            AccessControlContext context,
            OutboundAccessControlForCustomClass outboundAccessControlSettings
    ) {
        if (accessControlEvaluator != null) {
            return accessControlEvaluator.anonymizeObjectIfNeeded(
                    targetObject,
                    context,
                    outboundAccessControlSettings
            );
        } else {
            return new AnonymizationResult<>(targetObject);
        }
    }

    private static void anonymizeField(Object targetObject,
                                       String fieldName) {
        try {
            ReflectionUtils.setFieldValueThrowing(targetObject, fieldName, null);
        } catch (Exception ex) {
            throw new AccessControlMisconfigurationException("Anonymization failed. Can't set a value for a field ." + fieldName, ex);
        }
    }

    public <DATA> DATA retrieveDataIfAllowed(AccessControlContext context,
                                             Supplier<DATA> dataSupplier,
                                             AccessControlModel inboundAccessControlRequirements) {
        if (inboundAccessControlRequirements == null
                || evaluateInboundRequirements(context, inboundAccessControlRequirements)
        ) {
            log.debug("Inbound Access is allowed for a request {}. Proceeding...", context.request());
            return dataSupplier.get();
        } else {
            log.debug("Inbound Access is not allowed for a request {}, returning empty response", context.request());
            return null;
        }
    }

    public <T> AnonymizationResult<T> anonymizeObjectIfNeeded(
            T targetObject,
            AccessControlContext context,
            OutboundAccessControlForCustomClass outboundAccessControlSettings
    ) {
        return anonymizeObjectIfNeeded("", targetObject, context, outboundAccessControlSettings);
    }

    private <T> AnonymizationResult<T> anonymizeObjectIfNeeded(
            String fieldPath,
            T targetObject,
            AccessControlContext context,
            OutboundAccessControlForCustomClass outboundAccessControlSettings
    ) {
        if (targetObject == null || outboundAccessControlSettings == null) {
            log.debug("Target object is either null or outbound access control settings are not specified. Access to the entire target object is allowed.");
            return new AnonymizationResult<>(
                    targetObject,
                    false,
                    Collections.emptySet()
            );
        }
        boolean isFullyAnonymized = !evaluateOutboundRequirements(
                context,
                outboundAccessControlSettings.getClassLevel()
        );
        if (isFullyAnonymized) {
            log.debug("Access to the entire {} is not allowed, anonymizing...", targetObject);
            return new AnonymizationResult<>(null, true, Collections.emptySet());
        }

        log.debug("Access to the entire {} is allowed, proceeding...", targetObject);
        Set<String> targetObjectAnonymizedFields = anonymizeFields(
                targetObject,
                context,
                outboundAccessControlSettings.getFieldLevel()
        );
        log.debug(
                "Anonymizing fields of the {}. {}",
                targetObject,
                targetObjectAnonymizedFields.isEmpty() ? "None fields have been anonymized." : "Fields anonymized: " + String.join(", ", targetObjectAnonymizedFields)
        );

        List<String> nestedAnonymizedPaths = new ArrayList<>();
        MapUtils.emptyIfNull(outboundAccessControlSettings.getNested())
                .forEach((fieldName, nestedOutboundAccessControlSettings) -> {
                    Object nestedTargetObject = ReflectionUtils.getFieldValueThrowing(targetObject, fieldName);
                    if (nestedTargetObject != null && !targetObjectAnonymizedFields.contains(fieldName)) {
                        AnonymizationResult<Object> anonymizationResult = anonymizeObjectIfNeeded(
                                fieldName,
                                nestedTargetObject,
                                context,
                                nestedOutboundAccessControlSettings
                        );
                        if (anonymizationResult.isFullyAnonymized()) {
                            nestedAnonymizedPaths.add(fieldName);
                            anonymizeField(targetObject, fieldName);
                        } else {
                            nestedAnonymizedPaths.addAll(
                                    anonymizationResult.anonymizedFields()
                            );
                        }
                    }
                });

        Set<String> anonymizedPaths = Stream.concat(
                        nestedAnonymizedPaths.stream(),
                        targetObjectAnonymizedFields.stream()
                )
                .map(p -> !fieldPath.isEmpty() ? fieldPath + "." + p : p)
                .collect(Collectors.toSet());

        return new AnonymizationResult<>(targetObject, false, anonymizedPaths);
    }

    private Set<String> anonymizeFields(
            Object targetObject,
            AccessControlContext context,
            Map<String, AccessControlModel> fieldLevelAcSettings
    ) {
        Set<String> anonymizedFields = new HashSet<>();
        if (fieldLevelAcSettings != null) {
            for (Map.Entry<String, AccessControlModel> e : fieldLevelAcSettings.entrySet()) {
                String fieldName = e.getKey();
                Object fieldValue = ReflectionUtils.getFieldValueThrowing(targetObject, fieldName);
                if (fieldValue != null) {
                    AccessControlModel fieldAcInfo = e.getValue();
                    if (!evaluateOutboundRequirements(context, fieldAcInfo)) {
                        anonymizeField(targetObject, fieldName);
                        anonymizedFields.add(fieldName);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(anonymizedFields);
    }

}
