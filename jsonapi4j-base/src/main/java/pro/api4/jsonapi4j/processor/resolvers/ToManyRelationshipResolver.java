package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;

/**
 * Functional interface that resolves the to-many relationship data for a single resource.
 * <p>
 * Invoked once per resource during the relationship-resolution phase. Use this resolver when
 * each resource can be handled independently. For batched resolution across all resources in
 * one call, see {@link BatchToManyRelationshipResolver}.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface ToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Resolves the to-many relationship object for the given resource.
     *
     * @param request       the current request
     * @param dataSourceDto the downstream resource DTO
     * @return the {@link ToManyRelationshipObject} representing the relationship; must not be {@code null}
     */
    ToManyRelationshipObject resolveRequestedData(REQUEST request,
                                                  DATA_SOURCE_DTO dataSourceDto);

}
