package pro.api4.jsonapi4j.processor.multi.relationship;

import pro.api4.jsonapi4j.processor.RelationshipJsonApiContext;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.MultipleDataItemsDocMetaResolver;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Processing context for the to-many relationship pipeline.
 * <p>
 * Extends {@link pro.api4.jsonapi4j.processor.RelationshipJsonApiContext} with top-level doc
 * resolvers specific to responses that carry a collection of resource linkage objects
 * (e.g. {@code GET /users/{id}/relationships/roles}).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream relationship DTO type
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO>
        extends RelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> {

    private MultipleDataItemsDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private MultipleDataItemsDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
