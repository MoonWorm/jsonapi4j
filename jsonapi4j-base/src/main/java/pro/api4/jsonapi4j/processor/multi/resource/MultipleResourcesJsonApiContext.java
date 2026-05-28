package pro.api4.jsonapi4j.processor.multi.resource;

import pro.api4.jsonapi4j.processor.ResourceJsonApiContext;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Processing context for the multiple-resources pipeline.
 * <p>
 * Extends {@link ResourceJsonApiContext} with top-level doc resolvers specific to responses
 * that carry a list of primary resources (e.g. {@code GET /users}).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 * @param <ATTRIBUTES>     the attributes object type
 */
@SuperBuilder
@Getter
public class MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private final MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
