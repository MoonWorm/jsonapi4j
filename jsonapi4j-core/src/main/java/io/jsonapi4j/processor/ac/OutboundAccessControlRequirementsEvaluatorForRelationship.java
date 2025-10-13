package io.jsonapi4j.processor.ac;

import io.jsonapi4j.model.document.data.ResourceIdentifierObject;
import io.jsonapi4j.plugin.ac.AccessControlEvaluator;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;

@AllArgsConstructor
@Data
public class OutboundAccessControlRequirementsEvaluatorForRelationship {

    private static final Logger log = LoggerFactory.getLogger(OutboundAccessControlRequirementsEvaluatorForRelationship.class);

    private final AccessControlEvaluator accessControlEvaluator;
    private OutboundAccessControlSettingsForRelationship outboundAccessControlSettings;

    public ResourceIdentifierObject anonymizeResourceIdentifierIfNeeded(ResourceIdentifierObject resourceIdentifier) {
        if (resourceIdentifier == null) {
            return null;
        }
        if (accessControlEvaluator.evaluateOutboundRequirements(
                resourceIdentifier,
                outboundAccessControlSettings.getForResourceIdentifier().getObjectLevel())
        ) {
            log.info("Access to the entire JSON:API resource identifier [{}] is allowed, proceeding...", resourceIdentifier);
            Set<String> anonymizedFields = accessControlEvaluator.anonymizeFieldValues(
                    resourceIdentifier,
                    resourceIdentifier,
                    outboundAccessControlSettings.getForResourceIdentifier().getFieldLevel()
            );
            if (CollectionUtils.isNotEmpty(anonymizedFields)) {
                log.warn("Outbound Access control evaluation (field-level) for resource identifier object {}: Anonymized fields: {}", resourceIdentifier, anonymizedFields);
            }
            return resourceIdentifier;
        } else {
            log.info("Access to the entire JSON:API resource identifier is not allowed, excluding...");
            return null;
        }
    }

}
