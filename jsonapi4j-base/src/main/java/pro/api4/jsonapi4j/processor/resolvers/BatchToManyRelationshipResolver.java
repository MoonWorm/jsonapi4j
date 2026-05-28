package pro.api4.jsonapi4j.processor.resolvers;

import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;

import java.util.List;
import java.util.Map;

/**
 * Functional interface that resolves to-many relationship data for multiple resources in one call.
 * <p>
 * Intended for cases where querying the relationship for each resource individually would be
 * inefficient (N+1 problem). Instead, all resource DTOs in the current response are passed
 * together, allowing the implementation to fetch the data in a single bulk operation
 * (e.g., one SQL {@code IN} query).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type used as the map key
 */
@FunctionalInterface
public interface BatchToManyRelationshipResolver<REQUEST, DATA_SOURCE_DTO> {

    /**
     * Resolves the to-many relationship for all resources in a single batched call.
     * <p>
     * Prefer this over {@link ToManyRelationshipResolver} when bulk-fetching relationship
     * data is more efficient than N individual calls (e.g., a single SQL {@code IN} query).
     *
     * @param request        the current request
     * @param dataSourceDtos the list of all downstream resource DTOs in the current response
     * @return a map from each resource DTO to its {@link ToManyRelationshipObject};
     *         every DTO in the input list must have a corresponding entry in the result
     */
    Map<DATA_SOURCE_DTO, ToManyRelationshipObject> resolveRequestedData(REQUEST request,
                                                                        List<DATA_SOURCE_DTO> dataSourceDtos);

}
