package pro.api4.jsonapi4j.response;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.response.pagination.LimitOffsetToCursorAdapter.DEFAULT_LIMIT;

public class PaginationAwareResponseTests {

    // --- empty ---

    @Test
    public void empty_returnsEmptyItemsAndNullCursor() {
        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.empty();

        // then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPaginationContext()).isNull();
    }

    // --- fromItemsAndCursor ---

    @Test
    public void cursorAware() {
        // given
        List<String> items = List.of("a", "b");
        String cursor = "xyz";

        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.cursorAware(items, cursor);

        // then
        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getPaginationContext().getNextCursor()).isEqualTo("xyz");
    }

    // --- fromItemsNotPageable ---

    @Test
    public void fromItemsNotPageable_nullItems_returnsEmptyList() {
        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.fromItemsNotPageable(null);

        // then
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getPaginationContext()).isNull();
    }

    @Test
    public void fromItemsNotPageable_itemsWithNulls_filtersNulls() {
        // given
        List<String> items = new ArrayList<>(Arrays.asList(null, "a", null, "b"));

        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.fromItemsNotPageable(items);

        // then
        assertThat(response.getItems()).containsExactly("a", "b");
        assertThat(response.getPaginationContext()).isNull();
    }

    @Test
    public void fromItemsNotPageable_normalItems_returnsAllItems() {
        // given
        List<String> items = List.of("a", "b", "c");

        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.fromItemsNotPageable(items);

        // then
        assertThat(response.getItems()).containsExactly("a", "b", "c");
        assertThat(response.getPaginationContext()).isNull();
    }

    // --- fromItemsPageable ---

    @Test
    public void inMemoryAndNullCursor() {
        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.inMemoryCursorAware(null, null, 3);

        // then
        assertThat(response.getItems()).isNull();
        assertThat(response.getPaginationContext()).isNull();
    }

    @Test
    public void inMemoryAndNonNullCursor() {
        // given
        List<String> items = generateItems(10);

        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.inMemoryCursorAware(items, null, 3);

        // then
        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems()).containsExactly("item-0", "item-1", "item-2");
        assertThat(response.getPaginationContext().getNextCursor()).isNotNull();
    }

    @Test
    public void inMemoryCursorAware_middlePage_returnsCorrectSlice() {
        // given
        List<String> items = generateItems(10);
        String firstPageCursor = PaginationAwareResponse.inMemoryCursorAware(items, null, 3).getPaginationContext().getNextCursor();

        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.inMemoryCursorAware(items, firstPageCursor, 3);

        // then
        assertThat(response.getItems()).hasSize(3);
        assertThat(response.getItems()).containsExactly("item-3", "item-4", "item-5");
        assertThat(response.getPaginationContext().getNextCursor()).isNotNull();
    }

    @Test
    public void inMemoryCursorAware_nullCursor_checkNextPage() {
        // given — walk through pages to reach the last one
        List<String> items = generateItems(4);
        String firstPageCursor = PaginationAwareResponse.inMemoryCursorAware(items, null, 3).getPaginationContext().getNextCursor();

        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.inMemoryCursorAware(items, firstPageCursor, 3);

        // then
        assertThat(response.getItems()).hasSize(1);
        assertThat(response.getItems()).containsExactly("item-3");
        assertThat(response.getPaginationContext().getNextCursor()).isNull();
    }

    @Test
    public void inMemoryCursorAware_customLimit_respectsLimit() {
        // given
        List<String> items = generateItems(5);

        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.inMemoryCursorAware(items, null, 2);

        // then
        assertThat(response.getItems()).hasSize(2);
    }

    @Test
    public void inMemoryCursorAware_defaultOverload_usesDefaultLimit() {
        // given
        List<String> items = generateItems(DEFAULT_LIMIT + 5);

        // when
        PaginationAwareResponse<String> response = PaginationAwareResponse.inMemoryCursorAware(items);

        // then
        assertThat(response.getItems()).hasSize((int) DEFAULT_LIMIT);
    }

    private static List<String> generateItems(long count) {
        return IntStream.range(0, (int) count)
                .mapToObj(i -> "item-" + i)
                .toList();
    }

}
