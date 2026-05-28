package pro.api4.jsonapi4j.processor.single.relationship;

import pro.api4.jsonapi4j.processor.RelationshipJsonApiContext;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocLinksResolver;
import pro.api4.jsonapi4j.processor.resolvers.SingleDataItemDocMetaResolver;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * Processing context for the to-one relationship pipeline.
 * <p>
 * Extends {@link pro.api4.jsonapi4j.processor.RelationshipJsonApiContext} with top-level doc
 * resolvers specific to responses that carry a single resource linkage object
 * (e.g. {@code GET /users/{id}/relationships/country}).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream relationship DTO type
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO>
        extends RelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> {

    private SingleDataItemDocLinksResolver<REQUEST, DATA_SOURCE_DTO> topLevelLinksResolver;
    private SingleDataItemDocMetaResolver<REQUEST, DATA_SOURCE_DTO> topLevelMetaResolver;

}
