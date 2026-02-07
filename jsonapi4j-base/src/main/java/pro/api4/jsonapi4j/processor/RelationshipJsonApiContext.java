package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import lombok.Data;

@Data
public abstract class RelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> {

    // resource id and type
    private ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdResolver;
    // meta
    private ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver;

}
