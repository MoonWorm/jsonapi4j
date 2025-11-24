package pro.api4.jsonapi4j;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocMetaResolver;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class BatchToOneRelationshipProcessor {

    private AccessControlEvaluator accessControlEvaluator;
    private AccessControlModel inboundAccessControlSettings;
    private OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings;

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
            AccessControlModel inboundAccessControlSettings
    ) {
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        return this;
    }

    public BatchToOneRelationshipProcessor outboundAccessControlSettings(
            OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings
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
        private final AccessControlModel inboundAccessControlSettings;
        private final OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings;

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
            if (CollectionUtils.isEmpty(resourceDtos)) {
                return Collections.emptyMap();
            }

            // validation
            Validate.notNull(dataSupplier);
            Validate.notNull(resourceIdentifierTypeAndIdResolver);

            // compose relationship requests
            Map<RESOURCE_DTO, REQUEST> resourceDtosToRelationshipRequestMap = resourceDtos
                    .stream()
                    .collect(Collectors.toMap(
                            dto -> dto,
                            dto -> relationshipRequestSupplier.create(
                                    originalRequest,
                                    dto
                            )
                    ));

            // filter out dtos that are not allowed based on the corresponding request
            final List<RESOURCE_DTO> resourceDtosFiltered;
            if (accessControlEvaluator != null && inboundAccessControlSettings != null) {
                resourceDtosFiltered = resourceDtosToRelationshipRequestMap.entrySet()
                        .stream()
                        .map(e ->
                                accessControlEvaluator.retrieveDataIfAllowed(
                                        e.getValue(),
                                        e::getKey,
                                        inboundAccessControlSettings
                                )
                        )
                        .filter(Objects::nonNull)
                        .toList();
            } else {
                resourceDtosFiltered = resourceDtos;
            }

            if (CollectionUtils.isEmpty(resourceDtosFiltered)) {
                log.debug("Inbound Access control evaluation is failed for all relationship requests. Returning empty map.");
                return Collections.emptyMap();
            }

            //
            // Resolve relationships in batch
            //
            Map<RESOURCE_DTO, RELATIONSHIP_DTO> responseMap =
                    DataRetrievalUtil.retrieveDataNullable(
                            () -> dataSupplier.get(originalRequest, resourceDtosFiltered)
                    );

            if (MapUtils.isEmpty(responseMap)) {
                log.debug("Resolve relationships in batch. Empty result. Returning empty map.");
                return Collections.emptyMap();
            }

            Map<RESOURCE_DTO, ToOneRelationshipDoc> result = new HashMap<>();
            for (RESOURCE_DTO resourceDto : resourceDtos) {
                RELATIONSHIP_DTO relationshipDto = responseMap.get(resourceDto);
                if (!responseMap.containsKey(resourceDto)) {
                    //
                    // Is not allowed
                    //
                    result.put(resourceDto, null);
                } else {
                    REQUEST relationshipRequest = resourceDtosToRelationshipRequestMap.get(resourceDto);

                    // doc-level links
                    LinksObject docLinks = topLevelLinksResolver != null
                            ? topLevelLinksResolver.resolve(relationshipRequest, relationshipDto)
                            : null;
                    // doc-level meta
                    Object docMeta = topLevelMetaResolver != null
                            ? topLevelMetaResolver.resolve(relationshipRequest, relationshipDto)
                            : null;

                    if (relationshipDto == null) {
                        //
                        // Is allowed but response is null
                        //
                        result.put(resourceDto, new ToOneRelationshipDoc(null, docLinks, docMeta));
                    } else {
                        //
                        // Is allowed and response is not null
                        //

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
                        if (accessControlEvaluator != null && outboundAccessControlSettings != null) {
                            resourceIdentifier = accessControlEvaluator.anonymizeObjectIfNeeded(
                                    resourceIdentifier,
                                    resourceIdentifier,
                                    outboundAccessControlSettings.toOutboundRequirementsForCustomClass()
                            ).targetObject();
                        }

                        // compose doc and add to the result map
                        result.put(resourceDto, new ToOneRelationshipDoc(resourceIdentifier, docLinks, docMeta));
                    }
                }
            }
            return Collections.unmodifiableMap(result);
        }

    }
}
