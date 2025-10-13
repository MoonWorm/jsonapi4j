package io.jsonapi4j.processor.ac;

import io.jsonapi4j.processor.multi.resource.MultipleResourcesTerminalStage.IntermediateResultItem;
import io.jsonapi4j.model.document.data.ResourceObject;
import io.jsonapi4j.plugin.ac.AccessControlEvaluator;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.apache.commons.collections4.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

@AllArgsConstructor
@Data
public class OutboundAccessControlRequirementsEvaluatorForResource {

    private static final Logger log = LoggerFactory.getLogger(OutboundAccessControlRequirementsEvaluatorForResource.class);

    private final AccessControlEvaluator accessControlEvaluator;
    private OutboundAccessControlSettingsForResource outboundAccessControlSettings;

    public void calculateEffectiveAccessControlSettings(Class<?> resourceClass,
                                                        Class<?> attClass) {
        OutboundAccessControlSettingsForResource fromAnnotations
                = OutboundAccessControlSettingsForResource.fromAnnotations(resourceClass, attClass);
        this.outboundAccessControlSettings = OutboundAccessControlSettingsForResource.merge(
                this.outboundAccessControlSettings,
                fromAnnotations
        );
    }

    public <RESOURCE extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>, ATTRIBUTES, RELATIONSHIPS> ResourceAnonymizationResult<RESOURCE, ATTRIBUTES, RELATIONSHIPS> anonymizeResourceIfNeeded(
            RESOURCE resource
    ) {
        boolean isAllowed = accessControlEvaluator.evaluateOutboundRequirements(
                resource,
                outboundAccessControlSettings.getForResource().getObjectLevel()
        );
        if (!isAllowed) {
            log.info("Access to the entire JSON:API resource is not allowed, excluding...");
            return new ResourceAnonymizationResult<>(resource, true, Collections.emptySet());
        }
        log.info("Access to the entire JSON:API resource is allowed, proceeding...");
        Set<String> anonymizedFields = accessControlEvaluator.anonymizeFieldValues(
                resource,
                resource,
                outboundAccessControlSettings.getForResource().getFieldLevel()
        );
        log.info(
                "Anonymizing fields of the JSON:API resource. Fields anonymized: {}",
                anonymizedFields.isEmpty() ? "NONE" : String.join(", ", anonymizedFields)
        );
        return new ResourceAnonymizationResult<>(resource, false, anonymizedFields);
    }

    public <DATA_SOURCE_DTO, ATTRIBUTES, RELATIONSHIPS, RESOURCE extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>> void bulkResolveAndAnonymizeRelationshipsIfNeeded(
            List<IntermediateResultItem<DATA_SOURCE_DTO, ATTRIBUTES, RELATIONSHIPS, RESOURCE>> resourceAnonymizationResults,
            Supplier<Map<DATA_SOURCE_DTO, RELATIONSHIPS>> bulkRelationshipsSupplier
    ) {
        if (CollectionUtils.isEmpty(resourceAnonymizationResults)) {
            return;
        }
        Map<DATA_SOURCE_DTO, RELATIONSHIPS> relationshipsMap
                = bulkRelationshipsSupplier.get();
        resourceAnonymizationResults
                .forEach(i -> {
                    ResourceAnonymizationResult<RESOURCE, ?, RELATIONSHIPS> resourceAnonymizationResult = i.resourceAnonymizationResult();
                    if (!resourceAnonymizationResult.anonymizedFields().contains("relationships")) {
                        RELATIONSHIPS relationships = relationshipsMap.get(i.dataSourceDto());
                        resourceAnonymizationResult.resource().setRelationships(relationships);
                    } else {
                        log.info("Access to the entire 'relationships' section for the JSON:API resource {} is allowed, proceeding...", resourceAnonymizationResult);
                    }
                });
    }

    public <RESOURCE extends ResourceObject<?, RELATIONSHIPS>, RELATIONSHIPS> void resolveAndAnonymizeRelationshipsIfNeeded(
            ResourceAnonymizationResult<RESOURCE, ?, RELATIONSHIPS> resourceAnonymizationResult,
            Supplier<RELATIONSHIPS> relationshipsSupplier
    ) {
        if (resourceAnonymizationResult == null
                || resourceAnonymizationResult.anonymizedFields() == null
                || resourceAnonymizationResult.resource() == null) {
            return;
        }
        if (!resourceAnonymizationResult.anonymizedFields().contains("relationships")) {
            log.info("Access to the entire 'relationships' section is allowed, proceeding...");
            RELATIONSHIPS relationships = relationshipsSupplier.get();
            resourceAnonymizationResult.resource().setRelationships(relationships);
        }
    }

    public <RESOURCE extends ResourceObject<ATTRIBUTES, ?>, ATTRIBUTES> void anonymizeAttributesIfNeeded(
            ResourceAnonymizationResult<RESOURCE, ATTRIBUTES, ?> resourceAnonymizationResult
    ) {
        if (resourceAnonymizationResult == null
                || resourceAnonymizationResult.resource() == null
                || resourceAnonymizationResult.anonymizedFields() == null) {
            return;
        }

        RESOURCE resource = resourceAnonymizationResult.resource();
        ATTRIBUTES att = resource.getAttributes();
        if (att == null) {
            return;
        }

        if (!resourceAnonymizationResult.anonymizedFields().contains("attributes")) {
            if (accessControlEvaluator.evaluateOutboundRequirements(
                    resource,
                    outboundAccessControlSettings.getForAttributes().getObjectLevel()
            )) {
                log.info("Access to the entire 'attributes' section is allowed, proceeding...");
                Set<String> anonymizedAttributesFields = accessControlEvaluator.anonymizeFieldValues(
                        att,
                        resource,
                        outboundAccessControlSettings.getForAttributes().getFieldLevel()
                );
                log.info(
                        "Anonymizing fields of the resource 'attributes'. Fields anonymized: {}",
                        anonymizedAttributesFields.isEmpty() ? "NONE" : String.join(", ", anonymizedAttributesFields)
                );
            } else {
                resource.setAttributes(null);
                log.info("Access to the entire 'attributes' section is not allowed, excluding...");
            }
        }
    }

    public record ResourceAnonymizationResult<RESOURCE extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>, ATTRIBUTES, RELATIONSHIPS>(
            RESOURCE resource,
            boolean isFullyAnonymized,
            Set<String> anonymizedFields
    ) {
    }

}
