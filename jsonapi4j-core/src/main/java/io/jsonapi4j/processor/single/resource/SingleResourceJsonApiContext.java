package io.jsonapi4j.processor.single.resource;

import io.jsonapi4j.processor.ResourceJsonApiContext;
import io.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import io.jsonapi4j.processor.resolvers.SingleDataItemDocMetaResolver;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private final SingleDataItemDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
