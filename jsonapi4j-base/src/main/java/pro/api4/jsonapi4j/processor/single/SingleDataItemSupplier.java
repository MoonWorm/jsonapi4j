package pro.api4.jsonapi4j.processor.single;

import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;

/**
 * Functional interface that retrieves a single downstream resource or relationship DTO
 * from the data source (e.g. database, external service).
 * <p>
 * Provided as a lambda in the processor's fluent builder chain. Serves as the entry point
 * into the application's data access layer for single-item operations
 * ({@code GET /users/1}, {@code POST /users}, to-one relationship reads).
 *
 * @param <REQUEST>       the request type
 * @param <DATA_ITEM_DTO> the downstream DTO type returned by the data source
 */
@FunctionalInterface
public interface SingleDataItemSupplier<REQUEST, DATA_ITEM_DTO> {

    /**
     * Fetches the single downstream DTO for the given request.
     *
     * @param request the current request
     * @return the DTO; must not be {@code null}
     * @throws DataRetrievalException if the underlying data retrieval fails
     */
    DATA_ITEM_DTO get(REQUEST request) throws DataRetrievalException;

}
