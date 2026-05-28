package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.processor.resolvers.ResourceMetaResolver;
import pro.api4.jsonapi4j.processor.resolvers.ResourceTypeAndIdResolver;
import lombok.Data;

/**
 * Base context class for relationship pipeline processors.
 * <p>
 * Holds the minimal resolver configuration needed to build relationship documents
 * ({@code GET /users/{id}/relationships/...}): the resource type-and-id resolver and a
 * per-resource meta resolver.
 * <p>
 * Subclasses add top-level doc resolvers specific to the to-one
 * ({@link pro.api4.jsonapi4j.processor.single.relationship.ToOneRelationshipJsonApiContext}) or
 * to-many
 * ({@link pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsJsonApiContext})
 * relationship pipeline.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream relationship DTO type
 */
@Data
public abstract class RelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> {

    /** Resolver that maps a relationship DTO to its JSON:API {@code "id"} and {@code "type"} members. */
    private ResourceTypeAndIdResolver<DATA_SOURCE_DTO> resourceTypeAndIdResolver;

    /** Resolver for the {@code "meta"} member of each resource linkage object in the relationship document. */
    private ResourceMetaResolver<REQUEST, DATA_SOURCE_DTO> resourceMetaResolver;

}
