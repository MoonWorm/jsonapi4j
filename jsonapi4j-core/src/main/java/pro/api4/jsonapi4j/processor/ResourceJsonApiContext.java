package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.processor.resolvers.AttributesResolver;
import pro.api4.jsonapi4j.processor.resolvers.BatchToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.BatchToOneRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.DefaultRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToManyRelationshipResolver;
import pro.api4.jsonapi4j.processor.resolvers.ToOneRelationshipResolver;
import pro.api4.jsonapi4j.domain.RelationshipName;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.util.Map;

@SuperBuilder
@Getter
public abstract class ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    // resource type and id supplier
    private final ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdResolver;

    // attributes
    private final AttributesResolver<DATA_SOURCE_DTO, ATTRIBUTES> attributesResolver;

    // relationships
    private final Map<RelationshipName, DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> defaultRelationshipResolvers;
    private final Map<RelationshipName, ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toManyRelationshipResolvers;
    private final Map<RelationshipName, BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToManyRelationshipResolvers;
    private final Map<RelationshipName, ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> toOneRelationshipResolvers;
    private final Map<RelationshipName, BatchToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO>> batchToOneRelationshipResolvers;
    private final Map<RelationshipName, RelationshipType> dataResolutionIsConfiguredFor;

    // links
    private final ResourceLinksResolver<REQUEST, DATA_SOURCE_DTO> resourceLinksResolver;

    // meta
    private final ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver;

}
