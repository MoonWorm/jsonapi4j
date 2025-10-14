package pro.api4.jsonapi4j.processor.multi.resource;

import pro.api4.jsonapi4j.processor.resolvers.AttributesResolver;
import pro.api4.jsonapi4j.processor.resolvers.BatchToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.BatchToOneRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToOneRelationshipResolver;
import pro.api4.jsonapi4j.domain.RelationshipName;

import java.util.Map;

public class MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> {

    private final AttributesResolver<DATA_SOURCE_DTO, ATTRIBUTES> attributesResolver;

    public MultipleResourcesAttributesAwareJsonApiConfigurationStage(MultipleResourcesJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO> from,
                                                                     AttributesResolver<DATA_SOURCE_DTO, ATTRIBUTES> attributesResolver) {
        super(from.getRequest(), from.getDataSupplier(), from.getProcessorContext());
        setTopLevelLinksResolver(from.getTopLevelLinksResolver());
        setTopLevelMetaResolver(from.getTopLevelMetaResolver());
        setResourceLinksResolver(from.getResourceLinksResolver());
        setResourceMetaResolver(from.getResourceMetaResolver());
        getDefaultRelationshipResolvers().putAll(from.getDefaultRelationshipResolvers());
        getToManyRelationshipResolvers().putAll(from.getToManyRelationshipResolvers());
        getToOneRelationshipResolvers().putAll(from.getToOneRelationshipResolvers());
        getBatchToManyRelationshipResolvers().putAll(from.getBatchToManyRelationshipResolvers());
        getBatchToOneRelationshipResolvers().putAll(from.getBatchToOneRelationshipResolvers());
        getRelationshipTypes().putAll(from.getRelationshipTypes());
        this.attributesResolver = attributesResolver;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> topLevelLinksResolver(MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver) {
        super.topLevelLinksResolver(topLevelLinksResolver);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> topLevelMetaResolver(MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver) {
        super.topLevelMetaResolver(topLevelMetaResolver);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> defaultRelationships(DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> defaultRelationshipResolver, RelationshipName... relationshipNames) {
        super.defaultRelationships(defaultRelationshipResolver, relationshipNames);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> defaultRelationships(Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers) {
        super.defaultRelationships(defaultRelationshipResolvers);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> toManyRelationshipResolver(RelationshipName relationshipName, ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> toManyRelationshipResolver) {
        super.toManyRelationshipResolver(relationshipName, toManyRelationshipResolver);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> toManyRelationshipResolvers(Map<RelationshipName, ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toManyRelationshipsResolver) {
        super.toManyRelationshipResolvers(toManyRelationshipsResolver);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> toOneRelationshipResolver(RelationshipName relationshipName, ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> toOneRelationshipResolver) {
        super.toOneRelationshipResolver(relationshipName, toOneRelationshipResolver);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> toOneRelationshipResolvers(Map<RelationshipName, ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toOneRelationshipResolvers) {
        super.toOneRelationshipResolvers(toOneRelationshipResolvers);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> batchToManyRelationshipResolver(RelationshipName relationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> batchToManyRelationshipsResolver) {
        super.batchToManyRelationshipResolver(relationshipName, batchToManyRelationshipsResolver);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> batchToManyRelationshipResolvers(Map<RelationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToManyRelationshipResolvers) {
        super.batchToManyRelationshipResolvers(batchToManyRelationshipResolvers);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> batchToOneRelationshipResolver(RelationshipName relationshipName, BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> batchToOneRelationshipResolver) {
        super.batchToOneRelationshipResolver(relationshipName, batchToOneRelationshipResolver);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> batchToOneRelationshipResolvers(Map<RelationshipName, BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToOneRelationshipResolvers) {
        super.batchToOneRelationshipResolvers(batchToOneRelationshipResolvers);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> resourceLinksResolver(ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> resourceLinksResolver) {
        super.resourceLinksResolver(resourceLinksResolver);
        return this;
    }

    @Override
    public MultipleResourcesAttributesAwareJsonApiConfigurationStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> resourceMetaResolver(ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver) {
        super.resourceMetaResolver(resourceMetaResolver);
        return this;
    }

    /**
     * Sets resource type and resource id resolver.
     *
     * @param resourceTypeAndIdResolver - function that retrieves unique resource id and type from {@link DATA_SOURCE_DTO}
     * @return self ref
     */
    @Override
    public MultipleResourcesTerminalStage<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> resourceTypeAndIdResolver(
            ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdResolver
    ) {
        super.setResourceTypeAndIdResolverInternal(resourceTypeAndIdResolver);
        return new MultipleResourcesTerminalStage<>(
                getRequest(),
                getDataSupplier(),
                getProcessorContext(),
                MultipleResourcesJsonApiContext.<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>builder()
                        .attributesResolver(attributesResolver)
                        .resourceTypeAndIdResolver(getResourceTypeAndIdResolver())
                        .defaultRelationshipResolvers(getDefaultRelationshipResolvers())
                        .toManyRelationshipResolvers(getToManyRelationshipResolvers())
                        .batchToManyRelationshipResolvers(getBatchToManyRelationshipResolvers())
                        .toOneRelationshipResolvers(getToOneRelationshipResolvers())
                        .batchToOneRelationshipResolvers(getBatchToOneRelationshipResolvers())
                        .dataResolutionIsConfiguredFor(getRelationshipTypes())
                        .topLevelLinksResolver(getTopLevelLinksResolver())
                        .resourceLinksResolver(getResourceLinksResolver())
                        .topLevelMetaResolver(getTopLevelMetaResolver())
                        .resourceMetaResolver(getResourceMetaResolver())
                        .build()
        );
    }

}
