package pro.api4.jsonapi4j.plugin.ac;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.diagnostics.AccessControlDiagnostics;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;
import pro.api4.jsonapi4j.plugin.ac.copy.ObjectCopier;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

    /**
     * Works out what the caller may see of the given object and returns an object that shows only that.
     *
     * <p>The input graph is never modified. When something has to be hidden, a copy of the object is built
     * with those fields blanked out, and the copy is returned in its place; when nothing has to be hidden,
     * the original instance is returned and nothing is allocated. Objects reached through this method are
     * owned by the application — a resolver is free to hand back a cached or shared instance — so nulling
     * fields on them would corrupt them for every later request.
     *
     * <p>Copies are made one level at a time as the recursion unwinds, so only objects on a path to a
     * hidden field are copied; everything else is carried over by reference.
     */
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
        Set<String> deniedFields = deniedFields(
                targetObject,
                context,
                outboundAccessControlSettings.getFieldLevel()
        );
        log.debug(
                "Anonymizing fields of the {}. {}",
                targetObject,
                deniedFields.isEmpty() ? "None fields have been anonymized." : "Fields anonymized: " + String.join(", ", deniedFields)
        );

        Map<String, Object> replacements = new HashMap<>();
        deniedFields.forEach(fieldName -> replacements.put(fieldName, null));

        List<String> nestedAnonymizedPaths = new ArrayList<>();
        MapUtils.emptyIfNull(outboundAccessControlSettings.getNested())
                .forEach((fieldName, nestedOutboundAccessControlSettings) -> {
                    Object nestedTargetObject = ReflectionUtils.getFieldValueThrowing(targetObject, fieldName);
                    if (nestedTargetObject != null && !deniedFields.contains(fieldName)) {
                        AnonymizationResult<Object> anonymizationResult = anonymizeObjectIfNeeded(
                                fieldName,
                                nestedTargetObject,
                                context,
                                nestedOutboundAccessControlSettings
                        );
                        if (anonymizationResult.isFullyAnonymized()) {
                            nestedAnonymizedPaths.add(fieldName);
                            replacements.put(fieldName, null);
                        } else {
                            nestedAnonymizedPaths.addAll(
                                    anonymizationResult.anonymizedFields()
                            );
                            if (anonymizationResult.targetObject() != nestedTargetObject) {
                                replacements.put(fieldName, anonymizationResult.targetObject());
                            }
                        }
                    }
                });

        Set<String> anonymizedPaths = Stream.concat(
                        nestedAnonymizedPaths.stream(),
                        deniedFields.stream()
                )
                .map(p -> !fieldPath.isEmpty() ? fieldPath + "." + p : p)
                .collect(Collectors.toSet());

        T resultObject = replacements.isEmpty() ? targetObject : redact(targetObject, replacements);
        return new AnonymizationResult<>(resultObject, false, anonymizedPaths);
    }

    /**
     * Applies the hidden fields, by copying rather than by damaging the object — except for the envelopes
     * the framework itself builds.
     *
     * <p>A {@link ResourceIdentifierObject} — which every {@code ResourceObject} is — is constructed fresh
     * for each response out of the parts a resolver returned, so writing to it can affect nothing but the
     * response being built, and it is edited in place. Its constructors take inherited fields, which makes
     * it uncopyable anyway.
     *
     * <p>Everything reachable inside such an envelope is different: attributes and the objects nested in
     * them come from the application and may be cached, shared or otherwise outlive the request, so those
     * are copied.
     */
    private static <T> T redact(T targetObject, Map<String, Object> replacements) {
        try {
            if (targetObject instanceof ResourceIdentifierObject) {
                replacements.forEach((fieldName, value) ->
                        ReflectionUtils.setFieldValueThrowing(targetObject, fieldName, value));
                return targetObject;
            }
            return ObjectCopier.copyWith(targetObject, replacements);
        } catch (Exception ex) {
            throw AccessControlDiagnostics.unredactable(
                    targetObject.getClass(), replacements.keySet(), ex);
        }
    }

    /**
     * Returns the names of the fields the caller may not see, without touching the object.
     */
    private Set<String> deniedFields(
            Object targetObject,
            AccessControlContext context,
            Map<String, AccessControlModel> fieldLevelAcSettings
    ) {
        Set<String> deniedFields = new HashSet<>();
        if (fieldLevelAcSettings != null) {
            for (Map.Entry<String, AccessControlModel> e : fieldLevelAcSettings.entrySet()) {
                String fieldName = e.getKey();
                Object fieldValue = ReflectionUtils.getFieldValueThrowing(targetObject, fieldName);
                if (fieldValue != null) {
                    AccessControlModel fieldAcInfo = e.getValue();
                    if (!evaluateOutboundRequirements(context, fieldAcInfo)) {
                        deniedFields.add(fieldName);
                    }
                }
            }
        }
        return Collections.unmodifiableSet(deniedFields);
    }

}
