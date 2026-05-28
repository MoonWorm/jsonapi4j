package pro.api4.jsonapi4j.processor.single.resource;

import pro.api4.jsonapi4j.processor.ResourceJsonApiContext;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocMetaResolver;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

/**
 * Processing context for the single-resource pipeline.
 * <p>
 * Extends {@link ResourceJsonApiContext} with top-level doc resolvers specific to responses
 * that carry a single primary resource (e.g. {@code GET /users/1}, {@code POST /users}).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 * @param <ATTRIBUTES>     the attributes object type
 */
@SuperBuilder
@Getter
public class SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends ResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> {

    private final SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private final SingleDataItemDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
