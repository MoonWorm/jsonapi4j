package pro.api4.jsonapi4j;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.processor.PluginSettings;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocMetaResolver;
import pro.api4.jsonapi4j.processor.single.relationship.ToOneRelationshipProcessor;
import pro.api4.jsonapi4j.processor.util.DataRetrievalUtil;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

class BatchToOneRelationshipProcessor {

    private List<PluginSettings> plugins = Collections.emptyList();

    <REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> BatchToOneRelationshipJsonApiConfigurationStage<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier(
            BatchSingleResourcesDataSupplier<REQUEST, RESOURCE_DTO, RELATIONSHIP_DTO> dataSupplier
    ) {
        return new BatchToOneRelationshipJsonApiConfigurationStage<>(
                dataSupplier,
                this
        );
    }

    public BatchToOneRelationshipProcessor plugins(
            List<PluginSettings> plugins
    ) {
        this.plugins = plugins;
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

        private final List<PluginSettings> plugins;

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

            this.plugins = processorConfigurations.plugins;

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

            //
            // Resolve relationships in batch
            //
            Map<RESOURCE_DTO, RELATIONSHIP_DTO> responseMap =
                    DataRetrievalUtil.retrieveDataNullable(
                            () -> dataSupplier.get(originalRequest, resourceDtos)
                    );

            if (MapUtils.isEmpty(responseMap)) {
                log.debug("Resolve relationships in batch. Empty result. Returning empty map.");
                return Collections.emptyMap();
            }

            return resourceDtosToRelationshipRequestMap.entrySet().stream()
                    .collect(Collectors.toUnmodifiableMap(
                            Map.Entry::getKey,
                            e -> {
                                return new ToOneRelationshipProcessor()
                                        .forRequest(e.getValue())
                                        .plugins(this.plugins)
                                        .dataSupplier(r -> responseMap.get(e.getKey()))
                                        .topLevelLinksResolver(this.topLevelLinksResolver)
                                        .topLevelMetaResolver(this.topLevelMetaResolver)
                                        .resourceIdentifierMetaResolver(this.resourceMetaResolver)
                                        .resourceTypeAndIdSupplier(this.resourceIdentifierTypeAndIdResolver)
                                        .toToOneRelationshipDoc();
                            }
                    ));
        }

    }
}
