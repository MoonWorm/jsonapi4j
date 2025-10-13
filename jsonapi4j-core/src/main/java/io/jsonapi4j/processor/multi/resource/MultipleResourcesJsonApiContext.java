package io.jsonapi4j.processor.multi.resource;

import io.jsonapi4j.processor.ResourceJsonApiContext;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import io.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private final MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
