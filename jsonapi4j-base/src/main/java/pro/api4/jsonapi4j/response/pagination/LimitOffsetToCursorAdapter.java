package pro.api4.jsonapi4j.response.pagination;

import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.exception.InvalidCursorException;
import io.seruco.encoding.base62.Base62;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

import static pro.api4.jsonapi4j.request.LimitOffsetAwareRequest.DEFAULT_LIMIT;

/**
 * Adapter that bridges limit-offset pagination (used by many downstream data sources)
 * and the cursor-based pagination model preferred by the JSON:API framework.
 * <p>
 * Cursors are encoded as Base62 strings of the form {@code "<limit>:<offset>"}
 * (e.g. {@code "20:40"} becomes a short opaque token). This lets operations backed by SQL
 * {@code LIMIT}/{@code OFFSET} queries expose a cursor-based API to clients without
 * requiring the downstream data source to natively support cursors.
 * <p>
 * Typical usage:
 * <pre>{@code
 * LimitOffsetToCursorAdapter adapter = new LimitOffsetToCursorAdapter(request);
 * LimitAndOffset lo = adapter.decodeLimitAndOffset();
 * List<Item> page = dao.findPage(lo.getLimit(), lo.getOffset());
 * String nextCursor = adapter.nextCursor(lo, totalItemCount);
 * return PaginationAwareResponse.cursorAware(page, nextCursor);
 * }</pre>
 *
 * @see pro.api4.jsonapi4j.response.PaginationAwareResponse#inMemoryCursorAware(java.util.List, String, long)
 */
public class LimitOffsetToCursorAdapter {

    private final String cursor;
    private long defaultLimit = DEFAULT_LIMIT;

    private static final Base62 BASE_62 = Base62.createInstance();

    /**
     * Creates an adapter from a raw (encoded) cursor string.
     *
     * @param cursorEncoded the Base62-encoded cursor, or {@code null} for the first page
     */
    public LimitOffsetToCursorAdapter(String cursorEncoded) {
        this.cursor = cursorEncoded;
    }

    /**
     * Creates an adapter from a request that implements {@link CursorAwareRequest}.
     *
     * @param request the current request; its {@link CursorAwareRequest#getCursor()} value is used
     */
    public LimitOffsetToCursorAdapter(CursorAwareRequest request) {
        this.cursor = request.getCursor();
    }

    /**
     * Overrides the default page size used when the cursor is {@code null} (first page).
     *
     * @param defaultLimit the page size to use in the absence of an encoded cursor
     * @return {@code this} for chaining
     */
    public LimitOffsetToCursorAdapter withDefaultLimit(long defaultLimit) {
        this.defaultLimit = defaultLimit;
        return this;
    }

    /**
     * Decodes the cursor into a {@link LimitAndOffset} pair.
     * <p>
     * If the cursor is blank, returns the first page using {@link #defaultLimit} and offset 0.
     *
     * @return the decoded limit and offset
     * @throws pro.api4.jsonapi4j.exception.InvalidCursorException if the cursor is non-blank but malformed
     */
    public LimitAndOffset decodeLimitAndOffset() {
        if (StringUtils.isBlank(this.cursor)) {
            return new LimitAndOffset(defaultLimit, 0);
        }
        String decodedCursor;
        try {
            decodedCursor = new String(BASE_62.decode(this.cursor.getBytes()));
        } catch (IllegalArgumentException e) {
            throw new InvalidCursorException(this.cursor);
        }
        String[] limitAndOffset = decodedCursor.split(":");
        if (limitAndOffset.length != 2 || StringUtils.isBlank(limitAndOffset[0]) || StringUtils.isBlank(limitAndOffset[1])) {
            throw new InvalidCursorException(this.cursor);
        }
        try {
            long actualLimit = Long.parseLong(limitAndOffset[0]);
            if (actualLimit != defaultLimit) {
                throw new InvalidCursorException(this.cursor);
            }
            return new LimitAndOffset(actualLimit, Integer.parseInt(limitAndOffset[1]));
        } catch (NumberFormatException e) {
            throw new InvalidCursorException(this.cursor);
        }
    }

    /**
     * Generates the next-page cursor given the current page and the total item count.
     *
     * @param limitAndOffset the current page's limit and offset
     * @param totalNumberOfItems the total number of items in the data set
     * @return the encoded cursor for the next page, or {@code null} if the current page is the last
     */
    public String nextCursor(LimitAndOffset limitAndOffset, long totalNumberOfItems) {
        long nextOffset = limitAndOffset.getOffset() + limitAndOffset.getLimit();
        if (nextOffset >= totalNumberOfItems) {
            return null;
        }
        return encodeCursor(limitAndOffset.getLimit(), nextOffset);
    }

    /**
     * Decodes the cursor first, then generates the next-page cursor given the total item count.
     *
     * @param totalNumberOfItems the total number of items in the data set
     * @return the encoded cursor for the next page, or {@code null} if the current page is the last
     */
    public String nextCursor(long totalNumberOfItems) {
        LimitAndOffset limitAndOffset = decodeLimitAndOffset();
        long nextOffset = limitAndOffset.getOffset() + limitAndOffset.getLimit();
        if (nextOffset >= totalNumberOfItems) {
            return null;
        }
        return encodeCursor(limitAndOffset.getLimit(), nextOffset);
    }

    /**
     * Only used for infinite data sets assuming we never reach the max index.
     *
     * @return next cursor
     */
    public String nextCursor() {
        return nextCursor(Long.MAX_VALUE);
    }

    /**
     * Encodes a limit and offset pair into a Base62 cursor string.
     *
     * @param limit  the page size
     * @param offset the page offset (number of items to skip)
     * @return a Base62-encoded cursor string representing the given limit and offset
     */
    public static String encodeCursor(long limit, long offset) {
        String value = String.format("%s:%s", limit, offset);
        return new String(BASE_62.encode(value.getBytes()));
    }

    /**
     * Immutable value object holding a decoded limit and offset pair.
     */
    @Data
    public static class LimitAndOffset {
        /** The page size (number of items to return). */
        private final long limit;
        /** The page offset (number of items to skip from the start of the result set). */
        private final long offset;
    }

}
