package pro.api4.jsonapi4j.response;

import lombok.Data;
import pro.api4.jsonapi4j.response.pagination.LimitOffsetToCursorAdapter;
import pro.api4.jsonapi4j.response.pagination.LimitOffsetToCursorAdapter.LimitAndOffset;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;
import static pro.api4.jsonapi4j.request.LimitOffsetAwareRequest.DEFAULT_LIMIT;

/**
 * Represents a pageable response. Supports 'Limit-offset' and 'Cursor' pagination strategies (see {@link PaginationMode}).
 * Provides various methods for constructing an instance of the response.
 *
 * @param <DATA_SOURCE_DTO> type of downstream dto (resource or relationship)
 */
@Data
public class PaginationAwareResponse<DATA_SOURCE_DTO> {

    private List<DATA_SOURCE_DTO> items;
    private PaginationContext paginationContext;

    private PaginationAwareResponse(List<DATA_SOURCE_DTO> items,
                                    PaginationMode paginationMode,
                                    String nextCursor,
                                    Long totalItems) {
        this.items = items;
        this.paginationContext = PaginationContext.builder()
                .mode(paginationMode)
                .nextCursor(nextCursor)
                .totalItems(totalItems)
                .build();
    }

    private PaginationAwareResponse(List<DATA_SOURCE_DTO> items,
                                    PaginationContext paginationContext) {
        this.items = items;
        this.paginationContext = paginationContext;
    }

    /**
     * Creates an empty response with an empty {@link #items} list and <code>null</code> pagination context.
     * <p>
     * This represents "zero results" — the query executed successfully but returned no items.
     * The processor pipeline runs all phases normally (including relationship plugin visitors)
     * with an empty data array. This is distinct from an operation returning <code>null</code>,
     * which signals "no data available" and causes the pipeline to short-circuit before
     * relationship resolution.
     *
     * @param <T> type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> empty() {
        return new PaginationAwareResponse<>(Collections.emptyList(), null);
    }

    /**
     * Creates a response from {@link #items}, but set <code>null</code> to a {@link #paginationContext}.
     * <p>
     * Can be used for cases when we know for sure that we never have more than several items based on
     * domain rules and limitations.
     * <p>
     * De facto, this covers scenarios when the response can be not pageable by nature.
     *
     * @param items list of downstream dtos (limited amount)
     * @param <T>   type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> fromItemsNotPageable(List<T> items) {
        return new PaginationAwareResponse<>(
                emptyIfNull(items).stream()
                        .filter(Objects::nonNull)
                        .toList(),
                null
        );
    }

    /**
     * Creates a response with both {@link #items} and {@link #paginationContext} populated with a totalItems info.
     *
     * @param items      list of downstream dtos
     * @param totalItems total items amount on the server
     * @param <T>        type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> limitOffsetAware(List<T> items,
                                                                  long totalItems) {
        return new PaginationAwareResponse<>(items, PaginationMode.LIMIT_OFFSET, null, totalItems);
    }

    /**
     * Creates a response from {@link #items}. Generates {@link #paginationContext} in memory (not server-side) with
     * a totalItems info available in {@link #paginationContext}.
     * <p>
     * Can be used for cases when we know that the amount of items is relatively small so we can neglect it and make
     * all the calculations in-memory.
     *
     * @param items  list of downstream dtos (relatively small amount)
     * @param limit  limit (in-memory page size)
     * @param offset current cursor (encoded)
     * @param <T>    type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> inMemoryLimitOffsetAware(List<T> items, long limit, long offset) {
        if (items == null) {
            return new PaginationAwareResponse<>(null, null);
        } else {
            long from = Math.min(offset, items.size());
            long to = Math.min(offset + limit, items.size());
            List<T> itemsTruncated = items.subList((int) from, (int) to);
            return new PaginationAwareResponse<>(
                    itemsTruncated,
                    PaginationMode.LIMIT_OFFSET,
                    null,
                    (long) items.size()
            );
        }
    }

    /**
     * Creates a response with both {@link #items} and {@link #paginationContext} populated with a nextCursor info.
     *
     * @param items      list of downstream dtos
     * @param nextCursor the next cursor string value (generated server-side)
     * @param <T>        type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> cursorAware(List<T> items,
                                                             String nextCursor) {
        return new PaginationAwareResponse<>(items, PaginationMode.CURSOR, nextCursor, null);
    }

    /**
     * Creates a response from {@link #items}. Generates {@link #paginationContext} in memory (not server-side) with
     * a nextCursor info using {@link LimitOffsetToCursorAdapter}.
     * <p>
     * Can be used for cases when we can't force a downstream server to generate a cursor, but we know that the amount
     * of items is relatively small so we can neglect it and make all the calculations in-memory.
     *
     * @param items         list of downstream dtos (relatively small amount)
     * @param cursorEncoded current cursor (encoded)
     * @param defaultLimit  default limit (in-memory page size)
     * @param <T>           type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> inMemoryCursorAware(List<T> items,
                                                                     String cursorEncoded,
                                                                     long defaultLimit) {
        if (items == null) {
            return new PaginationAwareResponse<>(null, null);
        } else {
            LimitOffsetToCursorAdapter adapter = new LimitOffsetToCursorAdapter(cursorEncoded).withDefaultLimit(defaultLimit);
            LimitAndOffset limitAndOffset = adapter.decodeLimitAndOffset();
            long from = Math.min(limitAndOffset.getOffset(), items.size());
            long to = Math.min(limitAndOffset.getOffset() + limitAndOffset.getLimit(), items.size());
            List<T> itemsTruncated = items.subList((int) from, (int) to);
            return new PaginationAwareResponse<>(
                    itemsTruncated,
                    PaginationMode.CURSOR,
                    adapter.nextCursor(limitAndOffset, items.size()),
                    (long) items.size()
            );
        }
    }

    /**
     * Variation of {@link #inMemoryCursorAware(List, String, long)} with default limit set to
     * {@link pro.api4.jsonapi4j.request.LimitOffsetAwareRequest#DEFAULT_LIMIT}.
     *
     * @param items         list of downstream dtos (relatively small amount)
     * @param cursorEncoded current cursor (encoded)
     * @param <T>           type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> inMemoryCursorAware(List<T> items,
                                                                     String cursorEncoded) {
        return inMemoryCursorAware(items, cursorEncoded, DEFAULT_LIMIT);
    }

    /**
     * Variation of {@link #inMemoryCursorAware(List, String, long)} with <code>null</code> cursor.
     * Can be used for creation of the first page only or when we want to expose only first N elements
     * for some reason.
     *
     * @param items        list of downstream dtos (relatively small amount)
     * @param defaultLimit default limit (in-memory page size)
     * @param <T>          type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> inMemoryCursorAware(List<T> items,
                                                                     long defaultLimit) {
        return inMemoryCursorAware(items, null, defaultLimit);
    }

    /**
     * Variation of {@link #inMemoryCursorAware(List, String, long)} with <code>null</code> cursor default limit set to
     * {@link pro.api4.jsonapi4j.request.LimitOffsetAwareRequest#DEFAULT_LIMIT}.
     * Can be used for creation of the first page only or when we want to expose only first N elements
     * for some reason (with default value).
     *
     * @param items list of downstream dtos (relatively small amount)
     * @param <T>   type of downstream dto
     * @return an instance of {@link PaginationAwareResponse}
     */
    public static <T> PaginationAwareResponse<T> inMemoryCursorAware(List<T> items) {
        return inMemoryCursorAware(items, DEFAULT_LIMIT);
    }

}
