package pro.api4.jsonapi4j;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.PluginSettings;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsProcessor;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BatchToManyRelationshipsProcessor {

    private List<PluginSettings> plugins;

    <REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> BatchToManyRelationshipsJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier(
            BatchMultipleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier
    ) {
        return new BatchToManyRelationshipsJsonApiConfigurationStage<>(
                dataSupplier,
                this
        );
    }

    public BatchToManyRelationshipsProcessor plugins(
            List<PluginSettings> plugins
    ) {
        this.plugins = plugins;
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

        private final List<PluginSettings> plugins;

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

            this.plugins = processorConfigurations.plugins;

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

            //
            // Resolve relationships in batch
            //
            Map<RESOURCE_DTO, CursorPageableResponse<RELATIONSHIP_DTO>> responseMap =
                    DataRetrievalUtil.retrieveDataNullable(
                            () -> dataSupplier.get(originalRequest, resourceDtos)
                    );

            if (MapUtils.isEmpty(responseMap)) {
                log.debug("Resolve relationships in batch. Empty result. Returning empty map ");
                return Collections.emptyMap();
            }

            return resourceDtosToRelationshipRequestMap.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            e -> {
                                return new ToManyRelationshipsProcessor()
                                        .forRequest(e.getValue())
                                        .plugins(this.plugins)
                                        .dataSupplier(r -> responseMap.get(e.getKey()))
                                        .topLevelLinksResolver(this.topLevelLinksResolver)
                                        .topLevelMetaResolver(this.topLevelMetaResolver)
                                        .resourceIdentifierMetaResolver(this.resourceMetaResolver)
                                        .resourceTypeAndIdSupplier(this.resourceIdentifierTypeAndIdResolver)
                                        .toToManyRelationshipsDoc();
                            }
                    ));
        }

    }
}
