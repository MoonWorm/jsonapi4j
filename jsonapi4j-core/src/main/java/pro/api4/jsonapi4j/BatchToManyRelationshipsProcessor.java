package pro.api4.jsonapi4j;

import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;
import pro.api4.jsonapi4j.processor.ac.InboundAccessControlSettings;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlRequirementsEvaluatorForRelationship;
import pro.api4.jsonapi4j.processor.ac.OutboundAccessControlSettingsForRelationship;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
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
import java.util.Objects;

class BatchToManyRelationshipsProcessor {

    private AccessControlEvaluator accessControlEvaluator
            = ResourceProcessorContext.DEFAULT_ACCESS_CONTROL_EVALUATOR;
    private InboundAccessControlSettings inboundAccessControlSettings
            = InboundAccessControlSettings.DEFAULT;
    private OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
            = OutboundAccessControlSettingsForRelationship.DEFAULT;

    <REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> BatchToManyRelationshipsJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier(
            BatchMultipleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier
    ) {
        return new BatchToManyRelationshipsJsonApiConfigurationStage<>(
                dataSupplier,
                this
        );
    }

    public BatchToManyRelationshipsProcessor accessControlEvaluator(
            AccessControlEvaluator accessControlEvaluator
    ) {
        this.accessControlEvaluator = accessControlEvaluator;
        return this;
    }

    public BatchToManyRelationshipsProcessor inboundAccessControlSettings(
            InboundAccessControlSettings inboundAccessControlSettings
    ) {
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        return this;
    }

    public BatchToManyRelationshipsProcessor outboundAccessControlSettings(
            OutboundAccessControlSettingsForRelationship outboundAccessControlSettings
    ) {
        this.outboundAccessControlSettings = outboundAccessControlSettings;
        return this;
    }

    @FunctionalInterface
    interface BatchMultipleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> {

        Map<RESOURCE_DTO, CursorPageableResponse<RELATIONSHIP_DTO>> get(
                REQUEST request,
                List<RESOURCE_DTO> dataSourceDtos
        ) throws DataRetrievalException;

    }

    static class BatchToManyRelationshipsJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> {

        private final BatchMultipleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier;
        private final BatchToManyRelationshipsProcessor processorConfigurations;

        private MultipleDataItemsDocLinksResolver<REQUEST, RELATIONSHIP_DTO> topLevelLinksResolver;
        private MultipleDataItemsDocMetaResolver<REQUEST, RELATIONSHIP_DTO> topLevelMetaResolver;
        private ResourceMetaResolver<REQUEST, RELATIONSHIP_DTO> resourceMetaResolver;
        private ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver;

        BatchToManyRelationshipsJsonApiConfigurationStage(
                BatchMultipleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier,
                BatchToManyRelationshipsProcessor processorConfigurations
        ) {
            this.dataSupplier = dataSupplier;
            this.processorConfigurations = processorConfigurations;
        }

        BatchToManyRelationshipsJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> topLevelLinksResolver(
                MultipleDataItemsDocLinksResolver<REQUEST, RELATIONSHIP_DTO> docLinksResolver
        ) {
            this.topLevelLinksResolver = docLinksResolver;
            return this;
        }

        BatchToManyRelationshipsJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> topLevelMetaResolver(
                MultipleDataItemsDocMetaResolver<REQUEST, RELATIONSHIP_DTO> docMetaResolver
        ) {
            this.topLevelMetaResolver = docMetaResolver;
            return this;
        }

        BatchToManyRelationshipsJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> resourceIdentifierMetaResolver(
                ResourceMetaResolver<REQUEST, RELATIONSHIP_DTO> resourceMetaResolver
        ) {
            this.resourceMetaResolver = resourceMetaResolver;
            return this;
        }

        BatchToManyRelationshipsTerminalStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver(
                ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver
        ) {
            this.resourceIdentifierTypeAndIdResolver = resourceIdentifierTypeAndIdResolver;
            return new BatchToManyRelationshipsTerminalStage<>(
                    dataSupplier,
                    processorConfigurations,
                    this
            );
        }

    }

    @Slf4j
    static class BatchToManyRelationshipsTerminalStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> {

        private final BatchMultipleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier;

        private final AccessControlEvaluator accessControlEvaluator;
        private final InboundAccessControlSettings inboundAccessControlSettings;
        private final OutboundAccessControlSettingsForRelationship outboundAccessControlSettings;

        private final MultipleDataItemsDocLinksResolver<REQUEST, RELATIONSHIP_DTO> topLevelLinksResolver;
        private final MultipleDataItemsDocMetaResolver<REQUEST, RELATIONSHIP_DTO> topLevelMetaResolver;
        private final ResourceMetaResolver<REQUEST, RELATIONSHIP_DTO> resourceMetaResolver;
        private final ResourceTypeAndIdResolver<RELATIONSHIP_DTO> resourceIdentifierTypeAndIdResolver;

        BatchToManyRelationshipsTerminalStage(
                BatchMultipleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier,
                BatchToManyRelationshipsProcessor processorConfigurations,
                BatchToManyRelationshipsJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> jsonApiConfigurations
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

        Map<RESOURCE_DTO, ToManyRelationshipsDoc> toManyRelationshipsDocBatch(
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
            Map<RESOURCE_DTO, CursorPageableResponse<RELATIONSHIP_DTO>> responseMap =
                    DataRetrievalUtil.retrieveDataLenient(
                            () -> dataSupplier.get(originalRequest, resourceDtosFiltered)
                    );

            if (MapUtils.isEmpty(responseMap)) {
                log.debug("Resolve relationships in batch. Empty result. Returning empty map ");
                return Collections.emptyMap();
            }

            Map<RESOURCE_DTO, ToManyRelationshipsDoc> result = new HashMap<>();

            resourceDtos.forEach(resourceDto -> {
                CursorPageableResponse<RELATIONSHIP_DTO> cursorPageableResponse = responseMap.get(resourceDto);
                if (cursorPageableResponse != null && CollectionUtils.isNotEmpty(cursorPageableResponse.getItems())) {

                    REQUEST relationshipRequest = resourceDtosToRelationshipRequestMap.get(resourceDto);

                    OutboundAccessControlRequirementsEvaluatorForRelationship outboundAcEvaluator
                            = new OutboundAccessControlRequirementsEvaluatorForRelationship(
                            this.accessControlEvaluator,
                            this.outboundAccessControlSettings
                    );

                    List<ResourceIdentifierObject> data = cursorPageableResponse
                            .getItems()
                            .stream()
                            .map(relationshipDto -> {

                                // id and type
                                IdAndType idAndType = resourceIdentifierTypeAndIdResolver.resolveTypeAndId(relationshipDto);

                                if (idAndType == null || idAndType.getId() == null || StringUtils.isBlank(idAndType.getId())) {
                                    log.warn(
                                            "Resolved from {} relationship dto resource identifier is null, has null 'type' or empty 'id' members. Skipping...",
                                            relationshipDto
                                    );
                                    return null;
                                }

                                // resource meta
                                Object resourceMeta = resourceMetaResolver != null
                                        ? resourceMetaResolver.resolve(relationshipRequest, relationshipDto)
                                        : null;

                                // compose resource identifier
                                ResourceIdentifierObject resourceIdentifier = new ResourceIdentifierObject(
                                        idAndType.getId(),
                                        idAndType.getType().getType(),
                                        resourceMeta
                                );

                                // anonymize if needed
                                return outboundAcEvaluator.anonymizeResourceIdentifierIfNeeded(resourceIdentifier);

                            })
                            .filter(Objects::nonNull)
                            .toList();

                    // doc-level links
                    LinksObject docLinks = topLevelLinksResolver != null
                            ? topLevelLinksResolver.resolve(relationshipRequest, cursorPageableResponse.getItems(), cursorPageableResponse.getNextCursor())
                            : null;
                    // doc-level meta
                    Object docMeta = topLevelMetaResolver != null
                            ? topLevelMetaResolver.resolve(relationshipRequest, cursorPageableResponse.getItems())
                            : null;
                    // compose doc and add to the result map
                    result.put(resourceDto, new ToManyRelationshipsDoc(data, docLinks, docMeta));
                } else {
                    result.put(resourceDto, null);
                }
            });

            return Collections.unmodifiableMap(result);
        }

    }
}
