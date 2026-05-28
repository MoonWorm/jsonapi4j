package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.BaseDoc;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;

/**
 * Functional interface that resolves a relationship for a single resource when no specific
 * to-one or to-many resolver has been registered for the relationship name.
 * <p>
 * Acts as a catch-all resolver that is consulted when a relationship must be included
 * in the response but neither a {@link ToOneRelationshipResolver} nor a
 * {@link ToManyRelationshipResolver} is configured for it. The relationship name is passed as
 * an argument so that a single implementation can handle multiple relationship types.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface DefaultRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Resolves the relationship object for the given resource and relationship name.
     *
     * @param relationshipName the name of the relationship being resolved
     * @param request          the current request
     * @param dataSourceDto    the downstream resource DTO
     * @return a {@link RelationshipObject} (to-one or to-many); must not be {@code null}
     */
    RelationshipObject resolveDefaultRelationship(RelationshipName relationshipName,
                                                  REQUEST request,
                                                  DATA_SOURCE_DTO dataSourceDto);

}
