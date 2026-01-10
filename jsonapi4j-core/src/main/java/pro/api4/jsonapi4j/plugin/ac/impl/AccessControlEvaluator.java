package pro.api4.jsonapi4j.plugin.ac.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;
import pro.api4.jsonapi4j.plugin.ac.impl.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.impl.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.impl.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.plugin.ac.impl.tier.DefaultAccessTierRegistry;

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

    public static AccessControlEvaluator createDefault() {
        return new DefaultAccessControlEvaluator(new DefaultAccessTierRegistry());
    }

    public static <REQUEST, DATA> DATA retrieveDataIfAllowed(AccessControlEvaluator accessControlEvaluator,
                                                             REQUEST request,
                                                             Supplier<DATA> dataSupplier,
                                                             AccessControlModel inboundAccessControlSettings) {
        if (accessControlEvaluator != null) {
            // retrieve downstream data if allowed
            return accessControlEvaluator.retrieveDataIfAllowed(
                    request,
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
            Object resourceObject,
            OutboundAccessControlForCustomClass outboundAccessControlSettings
    ) {
        if (accessControlEvaluator != null) {
            return accessControlEvaluator.anonymizeObjectIfNeeded(
                    targetObject,
                    resourceObject,
                    outboundAccessControlSettings
            );
        } else {
            return new AnonymizationResult<>(targetObject);
        }
    }

    private static void anonymizeField(Object targetObject,
                                       String fieldName) {
        try {
            ReflectionUtils.setFieldValue(targetObject, fieldName, null);
        } catch (Exception ex) {
            throw new AccessControlMisconfigurationException("Anonymization failed. Can't set a value for a field ." + fieldName, ex);
        }
    }

    public <REQUEST, DATA> DATA retrieveDataIfAllowed(REQUEST request,
                                                      Supplier<DATA> dataSupplier,
                                                      AccessControlModel inboundAccessControlRequirements) {
        if (inboundAccessControlRequirements == null
                || evaluateInboundRequirements(request, inboundAccessControlRequirements)
        ) {
            log.info("Inbound Access is allowed for a request {}. Proceeding...", request);
            return dataSupplier.get();
        } else {
            log.info("Inbound Access is not allowed for a request {}, returning empty response", request);
            return null;
        }
    }

    public <T> AnonymizationResult<T> anonymizeObjectIfNeeded(
            T targetObject,
            Object resourceObject,
            OutboundAccessControlForCustomClass outboundAccessControlSettings
    ) {
        return anonymizeObjectIfNeeded("", targetObject, resourceObject, outboundAccessControlSettings);
    }

    private <T> AnonymizationResult<T> anonymizeObjectIfNeeded(
            String fieldPath,
            T targetObject,
            Object resourceObject,
            OutboundAccessControlForCustomClass outboundAccessControlSettings
    ) {
        if (targetObject == null || outboundAccessControlSettings == null) {
            log.info("Target object is either null or outbound access control settings are not specified. Access to the entire target object is allowed.");
            return new AnonymizationResult<>(
                    targetObject,
                    false,
                    Collections.emptySet()
            );
        }
        boolean isFullyAnonymized = !evaluateOutboundRequirements(
                resourceObject,
                outboundAccessControlSettings.getClassLevel()
        );
        if (isFullyAnonymized) {
            log.info("Access to the entire {} is not allowed, anonymizing...", targetObject);
            return new AnonymizationResult<>(null, true, Collections.emptySet());
        }

        log.info("Access to the entire {} is allowed, proceeding...", targetObject);
        Set<String> targetObjectAnonymizedFields = anonymizeFields(
                targetObject,
                resourceObject,
                outboundAccessControlSettings.getFieldLevel()
        );
        log.info(
                "Anonymizing fields of the {}. {}",
                targetObject,
                targetObjectAnonymizedFields.isEmpty() ? "None fields have been anonymized." : "Fields anonymized: " + String.join(", ", targetObjectAnonymizedFields)
        );

        List<String> nestedAnonymizedPaths = new ArrayList<>();
        MapUtils.emptyIfNull(outboundAccessControlSettings.getNested())
                .forEach((fieldName, nestedOutboundAccessControlSettings) -> {
                    Object nestedTargetObject = ReflectionUtils.getFieldValue(targetObject, fieldName);
                    if (nestedTargetObject != null && !targetObjectAnonymizedFields.contains(fieldName)) {
                        AnonymizationResult<Object> anonymizationResult = anonymizeObjectIfNeeded(
                                fieldName,
                                nestedTargetObject,
                                resourceObject,
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
            Object resourceObject,
            Map<String, AccessControlModel> fieldLevelAcSettings
    ) {
        Set<String> anonymizedFields = new HashSet<>();
        if (fieldLevelAcSettings != null) {
            for (Map.Entry<String, AccessControlModel> e : fieldLevelAcSettings.entrySet()) {
                String fieldName = e.getKey();
                Object fieldValue = ReflectionUtils.getFieldValue(targetObject, fieldName);
                if (fieldValue != null) {
                    AccessControlModel fieldAcInfo = e.getValue();
                    if (!evaluateOutboundRequirements(resourceObject, fieldAcInfo)) {
                        anonymizeField(targetObject, fieldName);
                        anonymizedFields.add(fieldName);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(anonymizedFields);
    }

}
