package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;

/**
 * Functional interface that resolves the to-one relationship data for a single resource.
 * <p>
 * Invoked once per resource during the relationship-resolution phase. Use this resolver when
 * each resource can be handled independently. For batched resolution across all resources in
 * one call, see {@link BatchToOneRelationshipResolver}.
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 */
@FunctionalInterface
public interface ToOneRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Resolves the to-one relationship object for the given resource.
     *
     * @param request       the current request
     * @param dataSourceDto the downstream resource DTO
     * @return the {@link ToOneRelationshipObject} representing the relationship; must not be {@code null}
     */
    ToOneRelationshipObject resolveRequestedData(REQUEST request,
                                                 DATA_SOURCE_DTO dataSourceDto);

}
