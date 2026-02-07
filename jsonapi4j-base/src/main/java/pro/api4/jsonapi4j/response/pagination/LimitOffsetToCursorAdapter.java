package pro.api4.jsonapi4j.response.pagination;

import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.processor.exception.InvalidCursorException;
import io.seruco.encoding.base62.Base62;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

public class LimitOffsetToCursorAdapter {

    public static final int DEFAULT_LIMIT = 10;

    private final String cursor;
    private int defaultLimit = DEFAULT_LIMIT;

    private static final Base62 BASE_62 = Base62.createInstance();

    public LimitOffsetToCursorAdapter(String cursorEncoded) {
        this.cursor = cursorEncoded;
    }

    public LimitOffsetToCursorAdapter(CursorAwareRequest request) {
        this.cursor = request.getCursor();
    }

    public LimitOffsetToCursorAdapter withDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
        return this;
    }

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
            int actualLimit = Integer.parseInt(limitAndOffset[0]);
            if (actualLimit != defaultLimit) {
                throw new InvalidCursorException(this.cursor);
            }
            return new LimitAndOffset(actualLimit, Integer.parseInt(limitAndOffset[1]));
        } catch (NumberFormatException e) {
            throw new InvalidCursorException(this.cursor);
        }
    }

    public String nextCursor(LimitAndOffset limitAndOffset, long totalNumberOfItems) {
        int nextOffset = limitAndOffset.getOffset() + limitAndOffset.getLimit();
        if (nextOffset >= totalNumberOfItems) {
            return null;
        }
        return encodeCursor(limitAndOffset.getLimit(), nextOffset);
    }

    public String nextCursor(long totalNumberOfItems) {
        LimitAndOffset limitAndOffset = decodeLimitAndOffset();
        int nextOffset = limitAndOffset.getOffset() + limitAndOffset.getLimit();
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

    static String encodeCursor(int limit, int offset) {
        String value = String.format("%s:%s", limit, offset);
        return new String(BASE_62.encode(value.getBytes()));
    }

    @Data
    public static class LimitAndOffset {
        private final int limit;
        private final int offset;
    }

}
