package pro.api4.jsonapi4j.processor.multi;

import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.processor.exception.DataRetrievalException;

/**
 * Functional interface that retrieves a collection of downstream resource DTOs
 * from the data source (e.g. database, external service).
 * <p>
 * Provided as a lambda in the processor's fluent builder chain. Serves as the entry point
 * into the application's data access layer for multi-item operations
 * ({@code GET /users}, to-many relationship reads).
 * The return type is {@link PaginationAwareResponse} so that the supplier can communicate
 * pagination cursors, total counts, and other pagination metadata alongside the items.
 *
 * @param <REQUEST>       the request type
 * @param <DATA_ITEM_DTO> the downstream DTO type returned by the data source
 */
@FunctionalInterface
public interface MultipleDataItemsSupplier<REQUEST, DATA_ITEM_DTO> {

    /**
     * Fetches the collection of downstream DTOs for the given request.
     *
     * @param request the current request
     * @return a {@link PaginationAwareResponse} containing the items and pagination metadata;
     *         must not be {@code null}
     * @throws DataRetrievalException if the underlying data retrieval fails
     */
    PaginationAwareResponse<DATA_ITEM_DTO> get(REQUEST request) throws DataRetrievalException;

}
