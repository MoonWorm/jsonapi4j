package io.jsonapi4j;

import io.jsonapi4j.processor.IdAndType;
import io.jsonapi4j.processor.ResourceProcessorContext;
import io.jsonapi4j.processor.ac.InboundAccessControlSettings;
import io.jsonapi4j.processor.ac.OutboundAccessControlRequirementsEvaluatorForRelationship;
import io.jsonapi4j.processor.ac.OutboundAccessControlSettingsForRelationship;
import io.jsonapi4j.processor.exception.DataRetrievalException;
import io.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import io.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import io.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import io.jsonapi4j.processor.resolvers.SingleDataItemDocMetaResolver;
import io.jsonapi4j.processor.util.DataRetrievalUtil;
import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.ResourceIdentifierObject;
import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import io.jsonapi4j.plugin.ac.AccessControlEvaluator;
import io.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import io.jsonapi4j.plugin.ac.tier.DefaultAccessTierRegistry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class BatchToOneRelationshipProcessor {

    private AccessControlEvaluator accessControlEvaluator
            = ResourceProcessorContext.DEFAULT_ACCESS_CONTROL_EVALUATOR;
    private InboundAccessControlSettings inboundAccessControlSettings
            = InboundAccessControlSettings.DEFAULT;
    private OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
            = OutboundAccessControlSettingsForRelationship.DEFAULT;

    <REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> BatchToOneRelationshipJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier(
            BatchSingleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier
    ) {
        return new BatchToOneRelationshipJsonApiConfigurationStage<>(
                dataSupplier,
                this
        );
    }

    public BatchToOneRelationshipProcessor accessControlEvaluator(
            AccessControlEvaluator accessControlEvaluator
    ) {
        this.accessControlEvaluator = accessControlEvaluator;
        return this;
    }

    public BatchToOneRelationshipProcessor inboundAccessControlSettings(
            InboundAccessControlSettings inboundAccessControlSettings
    ) {
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        return this;
    }

    public BatchToOneRelationshipProcessor outboundAccessControlSettings(
            OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
    ) {
        this.outboundAccessControlSettings = outboundAccessControlSettings;
        return this;
    }

    @FunctionalInterface
    interface BatchSingleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> {

        Map<RESOURCE_DTO, RELATIONSHIP_DTO> get(
                REQUEST request,
                List<RESOURCE_DTO> dataSourceDtos
        ) throws DataRetrievalException;

    }

    static class BatchToOneRelationshipJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> {

        private final BatchSingleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier;
        private final BatchToOneRelationshipProcessor processorConfigurations;

        private SingleDataItemDocLinksResolver<REQUEST, RELATIONSHIP_DTO> topLevelLinksResolver;
        private SingleDataItemDocMetaResolver<REQUEST, RELATIONSHIP_DTO> topLevelMetaResolver;
        private ResourceMetaResolver<REQUEST, RELATIONSHIP_DTO> resourceMetaResolver;
        private ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver;

        BatchToOneRelationshipJsonApiConfigurationStage(
                BatchSingleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier,
                BatchToOneRelationshipProcessor processorConfigurations
        ) {
            this.dataSupplier = dataSupplier;
            this.processorConfigurations = processorConfigurations;
        }

        BatchToOneRelationshipJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> topLevelLinksResolver(
                SingleDataItemDocLinksResolver<REQUEST, RELATIONSHIP_DTO> docLinksResolver
        ) {
            this.topLevelLinksResolver = docLinksResolver;
            return this;
        }

        BatchToOneRelationshipJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> topLevelMetaResolver(
                SingleDataItemDocMetaResolver<REQUEST, RELATIONSHIP_DTO> docMetaResolver
        ) {
            this.topLevelMetaResolver = docMetaResolver;
            return this;
        }

        BatchToOneRelationshipJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> resourceIdentifierMetaResolver(
                ResourceMetaResolver<REQUEST, RELATIONSHIP_DTO> resourceMetaResolver
        ) {
            this.resourceMetaResolver = resourceMetaResolver;
            return this;
        }

        BatchToOneRelationshipTerminalStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver(
                ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver
        ) {
            this.resourceIdentifierTypeAndIdResolver = resourceIdentifierTypeAndIdResolver;
            return new BatchToOneRelationshipTerminalStage<>(
                    dataSupplier,
                    processorConfigurations,
                    this
            );
        }

    }

    @Slf4j
    static class BatchToOneRelationshipTerminalStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> {

        private final BatchSingleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier;

        private final AccessControlEvaluator accessControlEvaluator;
        private final InboundAccessControlSettings inboundAccessControlSettings;
        private final OutboundAccessControlSettingsForRelationship outboundAccessControlSettings;

        private final SingleDataItemDocLinksResolver<REQUEST, RELATIONSHIP_DTO> topLevelLinksResolver;
        private final SingleDataItemDocMetaResolver<REQUEST, RELATIONSHIP_DTO> topLevelMetaResolver;
        private final ResourceMetaResolver<REQUEST, RELATIONSHIP_DTO> resourceMetaResolver;
        private final ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver;

        BatchToOneRelationshipTerminalStage(
                BatchSingleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier,
                BatchToOneRelationshipProcessor processorConfigurations,
                BatchToOneRelationshipJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> jsonApiConfigurations
        ) {
            this.dataSupplier = dataSupplier;

            this.accessControlEvaluator = processorConfigurations.accessControlEvaluator;
            this.inboundAccessControlSettings = processorConfigurations.inboundAccessControlSettings;
            this.outboundAccessControlSettings = processorConfigurations.outboundAccessControlSettings;

            this.topLevelLinksResolver = jsonApiConfigurations.topLevelLinksResolver;
            this.topLevelMetaResolver = jsonApiConfigurations.topLevelMetaResolver;
            this.resourceMetaResolver = jsonApiConfigurations.resourceMetaResolver;
            this.resourceIdentifierTypeAndIdResolver = jsonApiConfigurations.resourceIdentifierTypeAndIdResolver;
        }

        Map<RESOURCE_DTO, ToOneRelationshipDoc> toOneRelationshipDocBatch(
                REQUEST originalRequest,
                List<RESOURCE_DTO> resourceDtos,
                RelationshipRequestSupplier<REQUEST, RESOURCE_DTO> relationshipRequestSupplier
        ) {
            if (resourceDtos == null || resourceDtos.isEmpty()) {
                return Collections.emptyMap();
            }

            // validation
            Validate.notNull(dataSupplier);
            Validate.notNull(resourceIdentifierTypeAndIdResolver);

            //
            // Inbound Access Control checks + retrieve data
            //
            List<RESOURCE_DTO> resourceDtosFiltered = new ArrayList<>();
            Map<RESOURCE_DTO, REQUEST> resourceDtosToRelationshipRequestMap = new HashMap<>();
            for (RESOURCE_DTO resourceDto : resourceDtos) {
                REQUEST relationshipRequest = relationshipRequestSupplier.create(
                        originalRequest,
                        resourceDto
                );
                if (accessControlEvaluator.evaluateInboundRequirements(
                        relationshipRequest,
                        inboundAccessControlSettings.getForRequest())
                ) {
                    resourceDtosToRelationshipRequestMap.put(resourceDto, relationshipRequest);
                    resourceDtosFiltered.add(resourceDto);
                } else {
                    log.warn(
                            "Inbound Access control evaluation for relationship request [{}] and [{}] resource dto is failed. Restricting access.",
                            relationshipRequest,
                            resourceDto
                    );
                }
            }
            if (CollectionUtils.isEmpty(resourceDtosFiltered)) {
                log.debug("Inbound Access control evaluation is failed for all relationship requests. Returning empty map.");
                return Collections.emptyMap();
            }

            //
            // Resolve relationships in batch
            //
            Map<RESOURCE_DTO, RELATIONSHIP_DTO> responseMap =
                    DataRetrievalUtil.retrieveDataLenient(
                            () -> dataSupplier.get(originalRequest, resourceDtosFiltered)
                    );

            if (MapUtils.isEmpty(responseMap)) {
                log.debug("Resolve relationships in batch. Empty result. Returning empty map ");
                return Collections.emptyMap();
            }

            Map<RESOURCE_DTO, ToOneRelationshipDoc> result = new HashMap<>();
            responseMap.forEach((resourceDto, relationshipDto) -> {

                REQUEST relationshipRequest = resourceDtosToRelationshipRequestMap.get(resourceDto);

                OutboundAccessControlRequirementsEvaluatorForRelationship outboundAcEvaluator
                        = new OutboundAccessControlRequirementsEvaluatorForRelationship(
                        this.accessControlEvaluator,
                        this.outboundAccessControlSettings
                );

                // id and type
                IdAndType idAndType = resourceIdentifierTypeAndIdResolver.resolveTypeAndId(relationshipDto);

                ResourceIdentifierObject resourceIdentifier;

                if (idAndType == null || idAndType.getId() == null || StringUtils.isBlank(idAndType.getId())) {
                    log.warn(
                            "Resolved from {} relationship dto resource identifier is null, has null 'type' or empty 'id' members. Skipping...",
                            relationshipDto
                    );
                    resourceIdentifier = null;
                } else {
                    // resource meta
                    Object resourceMeta = resourceMetaResolver != null
                            ? resourceMetaResolver.resolve(relationshipRequest, relationshipDto)
                            : null;
                    // compose resource identifier
                    resourceIdentifier = new ResourceIdentifierObject(
                            idAndType.getId(),
                            idAndType.getType().getType(),
                            resourceMeta
                    );
                }

                // anonymize if needed
                resourceIdentifier = outboundAcEvaluator.anonymizeResourceIdentifierIfNeeded(resourceIdentifier);

                // doc-level links
                LinksObject docLinks = topLevelLinksResolver != null
                        ? topLevelLinksResolver.resolve(relationshipRequest, relationshipDto)
                        : null;
                // doc-level meta
                Object docMeta = topLevelMetaResolver != null
                        ? topLevelMetaResolver.resolve(relationshipRequest, relationshipDto)
                        : null;
                // compose doc and add to the result map
                result.put(resourceDto, new ToOneRelationshipDoc(resourceIdentifier, docLinks, docMeta));
            });
            return Collections.unmodifiableMap(result);
        }

    }
}
