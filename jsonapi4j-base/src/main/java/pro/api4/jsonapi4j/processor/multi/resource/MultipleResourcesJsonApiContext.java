package pro.api4.jsonapi4j.processor.multi.resource;

import pro.api4.jsonapi4j.processor.ResourceJsonApiContext;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public class MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private final MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
