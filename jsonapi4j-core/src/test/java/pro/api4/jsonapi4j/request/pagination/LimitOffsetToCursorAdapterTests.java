package pro.api4.jsonapi4j.request.pagination;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class LimitOffsetToCursorAdapterTests {

    @Test
    public void nextCursor_cursorIsNullAndRelyOnDefault_validateNextCursor() {
        // given
        String cursorEncoded = null;
        int defaultLimit = 5;

        // when
        String nextCursor = new LimitOffsetToCursorAdapter(cursorEncoded)
                .withDefaultLimit(defaultLimit)
                .nextCursor(defaultLimit + 100);

        // then
        assertThat(nextCursor).isNotNull().isEqualTo("EdT3");
    }

    @Test
    public void nextCursor_cursorIsSpecified_validateNextCursor() {
        // given
        int limit = 2;
        int offset = 0;
        String cursorEncoded = LimitOffsetToCursorAdapter.encodeCursor(limit, offset);

        // when
        String nextCursor = new LimitOffsetToCursorAdapter(cursorEncoded).withDefaultLimit(limit).nextCursor(10);

        // then
        assertThat(nextCursor).isNotNull().isEqualTo("DoJu");
    }

    @Test
    public void nextCursor_cursorIsSpecifiedAndLastPage_validateNextCursorIsNull() {
        // given
        int limit = 10;
        int offset = 10;
        String cursorEncoded = LimitOffsetToCursorAdapter.encodeCursor(limit, offset);

        // when
        String nextCursor = new LimitOffsetToCursorAdapter(cursorEncoded).withDefaultLimit(limit).nextCursor(10);

        // then
        assertThat(nextCursor).isNull();
    }

}
