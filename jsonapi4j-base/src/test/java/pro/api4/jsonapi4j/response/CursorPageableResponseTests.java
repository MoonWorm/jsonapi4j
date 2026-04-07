package pro.api4.jsonapi4j.response;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.response.pagination.LimitOffsetToCursorAdapter.DEFAULT_LIMIT;

public class CursorPageableResponseTests {

    // --- empty ---

    @Test
    public void empty_returnsEmptyItemsAndNullCursor() {
        // when
        CursorPageableResponse<String> response = CursorPageableResponse.empty();

        // then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
    }

    // --- fromItemsAndCursor ---

    @Test
    public void fromItemsAndCursor_returnsGivenItemsAndCursor() {
        // given
        List<String> items = List.of("a", "b");
        String cursor = "xyz";

        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsAndCursor(items, cursor);

        // then
        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getNextCursor()).isEqualTo("xyz");
    }

    // --- fromItemsNotPageable ---

    @Test
    public void fromItemsNotPageable_nullItems_returnsEmptyList() {
        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsNotPageable(null);

        // then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    public void fromItemsNotPageable_itemsWithNulls_filtersNulls() {
        // given
        List<String> items = new ArrayList<>(Arrays.asList(null, "a", null, "b"));

        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsNotPageable(items);

        // then
        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    public void fromItemsNotPageable_normalItems_returnsAllItems() {
        // given
        List<String> items = List.of("a", "b", "c");

        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsNotPageable(items);

        // then
        assertThat(response.getItems()).containsExactly("a", "b", "c");
        assertThat(response.getNextCursor()).isNull();
    }

    // --- fromItemsPageable ---

    @Test
    public void fromItemsPageable_nullItems_returnsNullItemsAndNullCursor() {
        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsPageable(null, null, 3);

        // then
        assertThat(response.getItems()).isNull();
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    public void fromItemsPageable_firstPage_returnsSlicedItemsAndNonNullCursor() {
        // given
        List<String> items = generateItems(10);

        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsPageable(items, null, 3);

        // then
        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems()).containsExactly("item-0", "item-1", "item-2");
        assertThat(response.getNextCursor()).isNotNull();
    }

    @Test
    public void fromItemsPageable_middlePage_returnsCorrectSlice() {
        // given
        List<String> items = generateItems(10);
        String firstPageCursor = CursorPageableResponse.fromItemsPageable(items, null, 3).getNextCursor();

        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsPageable(items, firstPageCursor, 3);

        // then
        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems()).containsExactly("item-3", "item-4", "item-5");
        assertThat(response.getNextCursor()).isNotNull();
    }

    @Test
    public void fromItemsPageable_lastPage_returnsRemainingItemsAndNullCursor() {
        // given — walk through pages to reach the last one
        List<String> items = generateItems(4);
        String firstPageCursor = CursorPageableResponse.fromItemsPageable(items, null, 3).getNextCursor();

        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsPageable(items, firstPageCursor, 3);

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems()).containsExactly("item-3");
        assertThat(response.getNextCursor()).isNull();
    }

    @Test
    public void fromItemsPageable_customLimit_respectsLimit() {
        // given
        List<String> items = generateItems(5);

        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsPageable(items, null, 2);

        // then
        assertThat(response.getItems()).hasSize(2);
    }

    @Test
    public void fromItemsPageable_defaultOverload_usesDefaultLimit() {
        // given
        List<String> items = generateItems(DEFAULT_LIMIT + 5);

        // when
        CursorPageableResponse<String> response = CursorPageableResponse.fromItemsPageable(items);

        // then
        assertThat(response.getItems()).hasSize((int) DEFAULT_LIMIT);
    }

    private static List<String> generateItems(long count) {
        return IntStream.range(0, (int) count)
                .mapToObj(i -> "item-" + i)
                .toList();
    }

}
