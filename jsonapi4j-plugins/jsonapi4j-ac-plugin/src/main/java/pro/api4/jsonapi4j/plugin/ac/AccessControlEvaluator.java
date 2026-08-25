package pro.api4.jsonapi4j.plugin.ac;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.anonymization.OutboundAnonymizer;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForCustomClass;

@Slf4j
public abstract class AccessControlEvaluator implements InboundAccessControlEvaluator, OutboundAccessControlEvaluator {

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

    public <T> AnonymizationResult<T> anonymizeObjectIfNeeded(
            T targetObject,
            AccessControlContext context,
            OutboundAccessControlForCustomClass outboundAccessControlSettings
    ) {
        return new OutboundAnonymizer(this::evaluateOutboundRequirements)
                .anonymize(targetObject, context, outboundAccessControlSettings);
    }

}
