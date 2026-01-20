package pro.api4.jsonapi4j;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.apache.commons.lang3.tuple.ImmutablePair;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.AnonymizationResult;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.IdAndType;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

class BatchToManyRelationshipsProcessor {

    private AccessControlEvaluator accessControlEvaluator;
    private AccessControlModel inboundAccessControlSettings;
    private OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings;

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
            AccessControlModel inboundAccessControlSettings
    ) {
        this.inboundAccessControlSettings = inboundAccessControlSettings;
        return this;
    }

    public BatchToManyRelationshipsProcessor outboundAccessControlSettings(
            OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings
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
        private final AccessControlModel inboundAccessControlSettings;
        private final OutboundAccessControlForJsonApiResourceIdentifier outboundAccessControlSettings;

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
            Map<RESOURCE_DTO, CursorPageableResponse<RELATIONSHIP_DTO>> responseMap =
                    DataRetrievalUtil.retrieveDataNullable(
                            () -> dataSupplier.get(originalRequest, resourceDtosFiltered)
                    );

            if (MapUtils.isEmpty(responseMap)) {
                log.debug("Resolve relationships in batch. Empty result. Returning empty map ");
                return Collections.emptyMap();
            }

            Map<RESOURCE_DTO, List<ImmutablePair<RELATIONSHIP_DTO, AnonymizationResult<ResourceIdentifierObject>>>> preComposedDataMap = responseMap.entrySet()
                    .stream()
                    .filter(e -> e.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            e -> preComposeData(
                                    resourceDtosToRelationshipRequestMap.get(e.getKey()),
                                    e.getValue()
                            ))
                    );

            return preComposedDataMap.entrySet().stream().collect(Collectors.toUnmodifiableMap(
                    Map.Entry::getKey,
                    e -> {
                        RESOURCE_DTO resourceDto = e.getKey();
                        List<ImmutablePair<RELATIONSHIP_DTO, AnonymizationResult<ResourceIdentifierObject>>> preComposedData = e.getValue();
                        REQUEST relationshipRequest = resourceDtosToRelationshipRequestMap.get(resourceDto);
                        return composeToManyRelationshipsDoc(relationshipRequest, responseMap.get(resourceDto), preComposedData);
                    }
            ));
        }

        private ToManyRelationshipsDoc composeToManyRelationshipsDoc(REQUEST relationshipRequest,
                                                                     CursorPageableResponse<RELATIONSHIP_DTO> cursorPageableResponse,
                                                                     List<ImmutablePair<RELATIONSHIP_DTO, AnonymizationResult<ResourceIdentifierObject>>> preComposedData) {

            List<RELATIONSHIP_DTO> nonAnonymizedRelationshipDtos = preComposedData.stream()
                    .filter(p -> p.getRight().isNotFullyAnonymized())
                    .map(ImmutablePair::getLeft)
                    .toList();

            String nextCursor = cursorPageableResponse.getNextCursor();

            // doc-level links
            LinksObject docLinks = topLevelLinksResolver != null
                    ? topLevelLinksResolver.resolve(relationshipRequest, nonAnonymizedRelationshipDtos, nextCursor)
                    : null;

            // doc-level meta
            Object docMeta = topLevelMetaResolver != null
                    ? topLevelMetaResolver.resolve(relationshipRequest, nonAnonymizedRelationshipDtos)
                    : null;

            List<ResourceIdentifierObject> data = preComposedData.stream()
                    .map(ImmutablePair::getRight)
                    .map(AnonymizationResult::targetObject)
                    .filter(Objects::nonNull)
                    .toList();

            return new ToManyRelationshipsDoc(data, docLinks, docMeta);
        }

        private List<ImmutablePair<RELATIONSHIP_DTO, AnonymizationResult<ResourceIdentifierObject>>> preComposeData(REQUEST request,
                                                                                                                    CursorPageableResponse<RELATIONSHIP_DTO> cursorPageableResponse) {
            return cursorPageableResponse
                    .getItems()
                    .stream()
                    .map(relationshipDto -> {
                        ResourceIdentifierObject resourceIdentifierObject = composeResourceIdentifierObject(
                                request,
                                relationshipDto
                        );
                        AnonymizationResult<ResourceIdentifierObject> anonymizationResult
                                = anonymizeIfNeeded(resourceIdentifierObject);
                        return new ImmutablePair<>(relationshipDto, anonymizationResult);
                    }).toList();
        }

        private ResourceIdentifierObject composeResourceIdentifierObject(REQUEST relationshipRequest, RELATIONSHIP_DTO relationshipDto) {
            // id and type
            IdAndType idAndType = resourceIdentifierTypeAndIdResolver.resolveTypeAndId(relationshipDto);

            // validate none of these is null
            Validate.notNull(idAndType);
            Validate.notNull(idAndType.getId());
            Validate.notNull(idAndType.getType());

            // resource meta
            Object resourceMeta = resourceMetaResolver != null
                    ? resourceMetaResolver.resolve(relationshipRequest, relationshipDto)
                    : null;

            // compose resource identifier
            return new ResourceIdentifierObject(
                    idAndType.getId(),
                    idAndType.getType().getType(),
                    resourceMeta
            );
        }

        private AnonymizationResult<ResourceIdentifierObject> anonymizeIfNeeded(ResourceIdentifierObject resourceIdentifierObject) {
            // anonymize if needed
            if (accessControlEvaluator != null && outboundAccessControlSettings != null) {
                return accessControlEvaluator.anonymizeObjectIfNeeded(
                        resourceIdentifierObject,
                        resourceIdentifierObject,
                        outboundAccessControlSettings.toOutboundRequirementsForCustomClass()
                );
            }
            return new AnonymizationResult<>(resourceIdentifierObject);
        }

    }
}
