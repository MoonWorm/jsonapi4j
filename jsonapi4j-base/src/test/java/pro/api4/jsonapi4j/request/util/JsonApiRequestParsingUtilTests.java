package pro.api4.jsonapi4j.request.util;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;
import pro.api4.jsonapi4j.request.SortAwareRequest;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JsonApiRequestParsingUtilTests {

    // --- parseFilter ---

    @Test
    void parseFilter_singleFilter_extractsNameAndValues() {
        // given
        Map<String, List<String>> params = Map.of("filter[region]", List.of("Europe,Americas"));

        // when
        Map<String, List<String>> result = JsonApiRequestParsingUtil.parseFilter(params);

        // then
        assertThat(result).containsKey("region");
        assertThat(result.get("region")).containsExactly("Americas", "Europe"); // sorted
    }

    @Test
    void parseFilter_multipleFilters_extractsAll() {
        // given
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("filter[region]", List.of("Europe"));
        params.put("filter[status]", List.of("active"));

        // when
        Map<String, List<String>> result = JsonApiRequestParsingUtil.parseFilter(params);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get("region")).containsExactly("Europe");
        assertThat(result.get("status")).containsExactly("active");
    }

    @Test
    void parseFilter_nonFilterParamsIgnored() {
        // given
        Map<String, List<String>> params = Map.of(
                "filter[id]", List.of("1,2"),
                "sort", List.of("-name"),
                "page[cursor]", List.of("abc")
        );

        // when
        Map<String, List<String>> result = JsonApiRequestParsingUtil.parseFilter(params);

        // then
        assertThat(result).containsOnlyKeys("id");
    }

    @Test
    void parseFilter_emptyParams_returnsEmptyMap() {
        // when
        Map<String, List<String>> result = JsonApiRequestParsingUtil.parseFilter(Collections.emptyMap());

        // then
        assertThat(result).isEmpty();
    }

    // --- parseSortBy ---

    @Test
    void parseSortBy_ascendingField() {
        // when
        Map<String, SortAwareRequest.SortOrder> result = JsonApiRequestParsingUtil.parseSortBy(List.of("name"));

        // then
        assertThat(result).containsEntry("name", SortAwareRequest.SortOrder.ASC);
    }

    @Test
    void parseSortBy_descendingField() {
        // when
        Map<String, SortAwareRequest.SortOrder> result = JsonApiRequestParsingUtil.parseSortBy(List.of("-createdAt"));

        // then
        assertThat(result).containsEntry("createdAt", SortAwareRequest.SortOrder.DESC);
    }

    @Test
    void parseSortBy_multipleFields() {
        // when
        Map<String, SortAwareRequest.SortOrder> result = JsonApiRequestParsingUtil.parseSortBy(List.of("region,-name"));

        // then
        assertThat(result).hasSize(2);
        assertThat(result).containsEntry("name", SortAwareRequest.SortOrder.DESC);
        assertThat(result).containsEntry("region", SortAwareRequest.SortOrder.ASC);
    }

    @Test
    void parseSortBy_explicitAscendingPrefix() {
        // when
        Map<String, SortAwareRequest.SortOrder> result = JsonApiRequestParsingUtil.parseSortBy(List.of("+name"));

        // then
        assertThat(result).containsEntry("name", SortAwareRequest.SortOrder.ASC);
    }

    @Test
    void parseSortBy_nullParam_returnsEmptyMap() {
        // when
        Map<String, SortAwareRequest.SortOrder> result = JsonApiRequestParsingUtil.parseSortBy(null);

        // then
        assertThat(result).isEmpty();
    }

    @Test
    void parseSortBy_exceedsGlobalCap_throwsConstraintViolation() {
        // when/then
        assertThatThrownBy(() ->
                JsonApiRequestParsingUtil.parseSortBy(List.of("a,b,c,d,e,f"))
        ).isInstanceOf(ConstraintViolationException.class);
    }

    // --- parseCursor ---

    @Test
    void parseCursor_extractsFirstValue() {
        // when
        String result = JsonApiRequestParsingUtil.parseCursor(List.of("DoJu"));

        // then
        assertThat(result).isEqualTo("DoJu");
    }

    @Test
    void parseCursor_nullParam_returnsNull() {
        // when
        String result = JsonApiRequestParsingUtil.parseCursor(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void parseCursor_emptyList_returnsNull() {
        // when
        String result = JsonApiRequestParsingUtil.parseCursor(Collections.emptyList());

        // then
        assertThat(result).isNull();
    }

    // --- parseLimit ---

    @Test
    void parseLimit_validNumber() {
        // when
        Long result = JsonApiRequestParsingUtil.parseLimit(List.of("50"));

        // then
        assertThat(result).isEqualTo(50L);
    }

    @Test
    void parseLimit_nullParam_returnsNull() {
        // when
        Long result = JsonApiRequestParsingUtil.parseLimit(null);

        // then
        assertThat(result).isNull();
    }

    // --- parseOffset ---

    @Test
    void parseOffset_validNumber() {
        // when
        Long result = JsonApiRequestParsingUtil.parseOffset(List.of("100"));

        // then
        assertThat(result).isEqualTo(100L);
    }

    @Test
    void parseOffset_nullParam_returnsNull() {
        // when
        Long result = JsonApiRequestParsingUtil.parseOffset(null);

        // then
        assertThat(result).isNull();
    }

    // --- parseEffectiveIncludes ---

    @Test
    void parseEffectiveIncludes_extractsFirstSegment() {
        // given — "citizenships.currencies" should extract "citizenships"
        // when
        List<String> result = JsonApiRequestParsingUtil.parseEffectiveIncludes(List.of("citizenships.currencies,placeOfBirth"));

        // then
        assertThat(result).containsExactly("citizenships", "placeOfBirth"); // sorted
    }

    @Test
    void parseEffectiveIncludes_simpleIncludes() {
        // when
        List<String> result = JsonApiRequestParsingUtil.parseEffectiveIncludes(List.of("relatives,citizenships"));

        // then
        assertThat(result).containsExactly("citizenships", "relatives"); // sorted
    }

    @Test
    void parseEffectiveIncludes_nullParam_returnsEmptyList() {
        // when
        List<String> result = JsonApiRequestParsingUtil.parseEffectiveIncludes(null);

        // then
        assertThat(result).isEmpty();
    }

    // --- parseOriginalIncludes ---

    @Test
    void parseOriginalIncludes_preservesFullPaths() {
        // when
        List<String> result = JsonApiRequestParsingUtil.parseOriginalIncludes(List.of("citizenships.currencies,placeOfBirth"));

        // then
        assertThat(result).containsExactly("citizenships.currencies", "placeOfBirth"); // sorted
    }

    // --- parseFieldSets ---

    @Test
    void parseFieldSets_extractsTypeAndFields() {
        // given
        Map<String, List<String>> params = Map.of(
                "fields[users]", List.of("email,name"),
                "fields[countries]", List.of("region")
        );

        // when
        Map<String, List<String>> result = JsonApiRequestParsingUtil.parseFieldSets(params);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get("users")).containsExactly("email", "name"); // sorted
        assertThat(result.get("countries")).containsExactly("region");
    }

    @Test
    void parseFieldSets_nonFieldsParamsIgnored() {
        // given
        Map<String, List<String>> params = Map.of(
                "fields[users]", List.of("email"),
                "filter[id]", List.of("1")
        );

        // when
        Map<String, List<String>> result = JsonApiRequestParsingUtil.parseFieldSets(params);

        // then
        assertThat(result).containsOnlyKeys("users");
    }

    // --- parseCustomQueryParams ---

    @Test
    void parseCustomQueryParams_excludesAllJsonApiParams() {
        // given
        Map<String, List<String>> params = new LinkedHashMap<>();
        params.put("filter[id]", List.of("1"));
        params.put("sort", List.of("name"));
        params.put("page[cursor]", List.of("abc"));
        params.put("page[limit]", List.of("20"));
        params.put("page[offset]", List.of("0"));
        params.put("include", List.of("citizenships"));
        params.put("fields[users]", List.of("email"));
        params.put("myCustomParam", List.of("hello"));

        // when
        Map<String, List<String>> result = JsonApiRequestParsingUtil.parseCustomQueryParams(params);

        // then
        assertThat(result).containsOnlyKeys("myCustomParam");
        assertThat(result.get("myCustomParam")).containsExactly("hello");
    }

    @Test
    void parseCustomQueryParams_emptyValues_excluded() {
        // given
        Map<String, List<String>> params = Map.of(
                "myParam", List.of(""),
                "otherParam", List.of("value")
        );

        // when
        Map<String, List<String>> result = JsonApiRequestParsingUtil.parseCustomQueryParams(params);

        // then
        assertThat(result).containsOnlyKeys("otherParam");
    }

    // --- parseResourceIdFromThePath ---

    @Test
    void parseResourceIdFromThePath_standardPath() {
        // when
        String result = JsonApiRequestParsingUtil.parseResourceIdFromThePath("/users/123");

        // then
        assertThat(result).isEqualTo("123");
    }

    @Test
    void parseResourceIdFromThePath_noId() {
        // when
        String result = JsonApiRequestParsingUtil.parseResourceIdFromThePath("/users");

        // then
        assertThat(result).isNull();
    }

    @Test
    void parseResourceIdFromThePath_nullPath() {
        // when
        String result = JsonApiRequestParsingUtil.parseResourceIdFromThePath(null);

        // then
        assertThat(result).isNull();
    }

    @Test
    void parseResourceIdFromThePath_relationshipPath() {
        // given — /users/123/relationships/citizenships
        // when
        String result = JsonApiRequestParsingUtil.parseResourceIdFromThePath("/users/123/relationships/citizenships");

        // then
        assertThat(result).isEqualTo("123");
    }

}
