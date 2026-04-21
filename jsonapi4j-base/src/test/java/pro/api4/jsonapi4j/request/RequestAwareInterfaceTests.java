package pro.api4.jsonapi4j.request;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RequestAwareInterfaceTests {

    // --- FiltersAwareRequest ---

    @Test
    void isJsonApiFilterParam_matchesFilterPattern() {
        assertThat(FiltersAwareRequest.isJsonApiFilterParam("filter[region]")).isTrue();
        assertThat(FiltersAwareRequest.isJsonApiFilterParam("filter[id]")).isTrue();
    }

    @Test
    void isJsonApiFilterParam_emptyBrackets_rejects() {
        assertThat(FiltersAwareRequest.isJsonApiFilterParam("filter[]")).isFalse();
    }

    @Test
    void isJsonApiFilterParam_rejectsNonFilterParams() {
        assertThat(FiltersAwareRequest.isJsonApiFilterParam("sort")).isFalse();
        assertThat(FiltersAwareRequest.isJsonApiFilterParam("page[cursor]")).isFalse();
        assertThat(FiltersAwareRequest.isJsonApiFilterParam("filter")).isFalse();
        assertThat(FiltersAwareRequest.isJsonApiFilterParam("filterregion")).isFalse();
    }

    @Test
    void extractFilterName_extractsNameFromBrackets() {
        assertThat(FiltersAwareRequest.extractFilterName("filter[region]")).isEqualTo("region");
        assertThat(FiltersAwareRequest.extractFilterName("filter[id]")).isEqualTo("id");
    }

    @Test
    void getFilterParam_formatsCorrectly() {
        assertThat(FiltersAwareRequest.getFilterParam("region")).isEqualTo("filter[region]");
    }

    @Test
    void getFilterParamWithValue_formatsWithCommaValues() {
        String result = FiltersAwareRequest.getFilterParamWithValue("id", List.of("1", "2", "3"));
        assertThat(result).isEqualTo("filter[id]=1,2,3");
    }

    // --- SortAwareRequest ---

    @Test
    void extractSortBy_stripsDescendingPrefix() {
        assertThat(SortAwareRequest.extractSortBy("-name")).isEqualTo("name");
    }

    @Test
    void extractSortBy_stripsAscendingPrefix() {
        assertThat(SortAwareRequest.extractSortBy("+name")).isEqualTo("name");
    }

    @Test
    void extractSortBy_noPrefix_returnsAsIs() {
        assertThat(SortAwareRequest.extractSortBy("name")).isEqualTo("name");
    }

    @Test
    void extractSortOrder_descendingPrefix() {
        assertThat(SortAwareRequest.extractSortOrder("-name")).isEqualTo(SortAwareRequest.SortOrder.DESC);
    }

    @Test
    void extractSortOrder_ascendingPrefix() {
        assertThat(SortAwareRequest.extractSortOrder("+name")).isEqualTo(SortAwareRequest.SortOrder.ASC);
    }

    @Test
    void extractSortOrder_noPrefix_defaultsToAsc() {
        assertThat(SortAwareRequest.extractSortOrder("name")).isEqualTo(SortAwareRequest.SortOrder.ASC);
    }

    @Test
    void wrapWithSortOrder_desc_addsDashPrefix() {
        assertThat(SortAwareRequest.wrapWithSortOrder("name", SortAwareRequest.SortOrder.DESC)).isEqualTo("-name");
    }

    @Test
    void wrapWithSortOrder_asc_noPrefixAdded() {
        assertThat(SortAwareRequest.wrapWithSortOrder("name", SortAwareRequest.SortOrder.ASC)).isEqualTo("name");
    }

    // --- PaginationAwareRequest ---

    @Test
    void isJsonApiPaginationParam_matchesPagePattern() {
        assertThat(PaginationAwareRequest.isJsonApiPaginationParam("page[cursor]")).isTrue();
        assertThat(PaginationAwareRequest.isJsonApiPaginationParam("page[limit]")).isTrue();
        assertThat(PaginationAwareRequest.isJsonApiPaginationParam("page[offset]")).isTrue();
    }

    @Test
    void isJsonApiPaginationParam_rejectsNonPageParams() {
        assertThat(PaginationAwareRequest.isJsonApiPaginationParam("sort")).isFalse();
        assertThat(PaginationAwareRequest.isJsonApiPaginationParam("filter[id]")).isFalse();
        assertThat(PaginationAwareRequest.isJsonApiPaginationParam("page")).isFalse();
    }

    // --- SparseFieldsetsAwareRequest ---

    @Test
    void isJsonApiFieldsParam_matchesFieldsPattern() {
        assertThat(SparseFieldsetsAwareRequest.isJsonApiFieldsParam("fields[users]")).isTrue();
        assertThat(SparseFieldsetsAwareRequest.isJsonApiFieldsParam("fields[countries]")).isTrue();
    }

    @Test
    void isJsonApiFieldsParam_rejectsNonFieldsParams() {
        assertThat(SparseFieldsetsAwareRequest.isJsonApiFieldsParam("fields")).isFalse();
        assertThat(SparseFieldsetsAwareRequest.isJsonApiFieldsParam("filter[id]")).isFalse();
    }

    @Test
    void extractResourceType_extractsTypeFromBrackets() {
        assertThat(SparseFieldsetsAwareRequest.extractResourceType("fields[users]")).isEqualTo("users");
    }

}
