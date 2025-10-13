package io.jsonapi4j.processor;

import io.jsonapi4j.request.pagination.LimitOffsetToCursorAdapter;
import io.jsonapi4j.request.pagination.LimitOffsetToCursorAdapter.LimitAndOffset;
import lombok.Data;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static org.apache.commons.collections4.ListUtils.emptyIfNull;

/**
 * Represents a pageable response. Implements Cursor-based pagination strategy. Provides various methods
 * for constructing an instance of the response.
 *
 * @param <DATA_SOURCE_DTO> type of downstream dto (resource or relationship)
 */
@Data
public class CursorPageableResponse<DATA_SOURCE_DTO> {

    private List<DATA_SOURCE_DTO> items;
    private String nextCursor;

    private CursorPageableResponse(List<DATA_SOURCE_DTO> items,
                                   String nextCursor) {
        this.items = items;
        this.nextCursor = nextCursor;
    }

    /**
     * Creates an empty response with an empty {@link #items} and <code>null</code> {@link #nextCursor}
     *
     * @param <T> type of downstream dto
     * @return an instance of {@link CursorPageableResponse}
     */
    public static <T> CursorPageableResponse<T> empty() {
        return new CursorPageableResponse<>(Collections.emptyList(), null);
    }

    /**
     * Creates a response with both {@link #items} and {@link #nextCursor} property populated.
     *
     * @param items      list of downstream dtos
     * @param nextCursor the next cursor string value (generated server-side)
     * @param <T>        type of downstream dto
     * @return an instance of {@link CursorPageableResponse}
     */
    public static <T> CursorPageableResponse<T> fromItemsAndCursor(List<T> items,
                                                                   String nextCursor) {
        return new CursorPageableResponse<>(items, nextCursor);
    }

    /**
     * Creates a response from {@link #items}, but set <code>null</code> to a {@link #nextCursor}.
     * <p>
     * Can be used for cases when we know for sure that we never have more than several items based on
     * domain rules and limitations.
     * <p>
     * De facto, this covers scenarios when the response can be not pageable by nature.
     *
     * @param items list of downstream dtos (limited amount)
     * @param <T>   type of downstream dto
     * @return an instance of {@link CursorPageableResponse}
     */
    public static <T> CursorPageableResponse<T> fromItemsNotPageable(List<T> items) {
        return new CursorPageableResponse<>(
                emptyIfNull(items).stream()
                        .filter(Objects::nonNull)
                        .toList(),
                null
        );
    }

    /**
     * Creates a response from {@link #items}. Generates {@link #nextCursor} in memory (not server-side) using
     * {@link LimitOffsetToCursorAdapter}.
     * <p>
     * Can be used for cases when we can't force a downstream server to generate a cursor, but we know that the amount
     * of items is relatively small so we can neglect it and make all the calculations in-memory.
     *
     * @param items         list of downstream dtos (relatively small amount)
     * @param cursorEncoded current cursor (encoded)
     * @param defaultLimit  default limit (in-memory page size)
     * @param <T>           type of downstream dto
     * @return an instance of {@link CursorPageableResponse}
     */
    public static <T> CursorPageableResponse<T> fromItemsPageable(List<T> items,
                                                                  String cursorEncoded,
                                                                  int defaultLimit) {
        if (items == null) {
            return new CursorPageableResponse<>(null, null);
        } else {
            LimitOffsetToCursorAdapter adapter = new LimitOffsetToCursorAdapter(cursorEncoded).withDefaultLimit(defaultLimit);
            LimitAndOffset limitAndOffset = adapter.decodeLimitAndOffset();
            int from = Math.min(limitAndOffset.getOffset(), items.size());
            int to = Math.min(limitAndOffset.getOffset() + limitAndOffset.getLimit(), items.size());
            List<T> itemsTruncated = items.subList(from, to);
            return new CursorPageableResponse<>(
                    itemsTruncated,
                    adapter.nextCursor(limitAndOffset, items.size())
            );
        }
    }

    /**
     * Variation of {@link #fromItemsPageable(List, String, int)} with default limit set to
     * {@link LimitOffsetToCursorAdapter#DEFAULT_LIMIT}.
     *
     * @param items         list of downstream dtos (relatively small amount)
     * @param cursorEncoded current cursor (encoded)
     * @param <T>           type of downstream dto
     * @return an instance of {@link CursorPageableResponse}
     */
    public static <T> CursorPageableResponse<T> fromItemsPageable(List<T> items,
                                                                  String cursorEncoded) {
        return fromItemsPageable(items, cursorEncoded, LimitOffsetToCursorAdapter.DEFAULT_LIMIT);
    }

    /**
     * Variation of {@link #fromItemsPageable(List, String, int)} with <code>null</code> cursor.
     * Can be used for creation of the first page only or when we want to expose only first N elements
     * for some reason.
     *
     * @param items        list of downstream dtos (relatively small amount)
     * @param defaultLimit default limit (in-memory page size)
     * @param <T>          type of downstream dto
     * @return an instance of {@link CursorPageableResponse}
     */
    public static <T> CursorPageableResponse<T> fromItemsPageable(List<T> items,
                                                                  int defaultLimit) {
        return fromItemsPageable(items, null, defaultLimit);
    }

    /**
     * Variation of {@link #fromItemsPageable(List, String, int)} with <code>null</code> cursor default limit set to
     * {@link LimitOffsetToCursorAdapter#DEFAULT_LIMIT}.
     * Can be used for creation of the first page only or when we want to expose only first N elements
     * for some reason (with default value).
     *
     * @param items list of downstream dtos (relatively small amount)
     * @param <T>   type of downstream dto
     * @return an instance of {@link CursorPageableResponse}
     */
    public static <T> CursorPageableResponse<T> fromItemsPageable(List<T> items) {
        return fromItemsPageable(items, LimitOffsetToCursorAdapter.DEFAULT_LIMIT);
    }

}
