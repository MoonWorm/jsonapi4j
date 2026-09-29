package pro.api4.jsonapi4j.compound.docs.cache;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

class CacheKeyTests {

    @Test
    void constructor_nullResource_throwsNullPointerException() {
        assertThatThrownBy(() -> new CacheKey(null, null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("idAndType must not be null");
    }

    @Test
    void constructor_nullResourceType_throwsNullPointerException() {
        assertThatThrownBy(() -> new CacheKey(idAndType(null, "1"), null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("resource type");
    }

    @Test
    void constructor_nullResourceId_throwsNullPointerException() {
        assertThatThrownBy(() -> new CacheKey(idAndType("countries", null), null, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessageContaining("resource id");
    }

    @Test
    void constructor_nullIncludes_treatedAsEmptySet() {
        CacheKey key = new CacheKey(idAndType("countries", "FI"), null, null);

        assertThat(key.getIncludes()).isEmpty();
    }

    @Test
    void constructor_nullFields_treatedAsEmptySet() {
        CacheKey key = new CacheKey(idAndType("countries", "FI"), null, null);

        assertThat(key.getFields()).isEmpty();
    }

    @Test
    void constructor_includesAreSorted_deterministicOrder() {
        Set<String> unordered = new LinkedHashSet<>();
        unordered.add("currencies");
        unordered.add("airports");
        unordered.add("languages");

        CacheKey key = new CacheKey(idAndType("countries", "FI"), unordered, null);

        assertThat(key.getIncludes()).containsExactly("airports", "currencies", "languages");
    }

    @Test
    void constructor_fieldsAreSorted_deterministicOrder() {
        Set<String> unordered = new LinkedHashSet<>();
        unordered.add("population");
        unordered.add("name");
        unordered.add("area");

        CacheKey key = new CacheKey(idAndType("countries", "FI"), null, unordered);

        assertThat(key.getFields()).containsExactly("area", "name", "population");
    }

    @Test
    void constructor_includesAreImmutable_throwsOnModification() {
        CacheKey key = new CacheKey(idAndType("countries", "FI"), Set.of("currencies"), null);

        assertThatThrownBy(() -> key.getIncludes().add("languages"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_fieldsAreImmutable_throwsOnModification() {
        CacheKey key = new CacheKey(idAndType("countries", "FI"), null, Set.of("name"));

        assertThatThrownBy(() -> key.getFields().add("population"))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void constructor_defensiveCopy_originalSetModificationDoesNotAffectKey() {
        Set<String> mutableIncludes = new HashSet<>();
        mutableIncludes.add("currencies");

        CacheKey key = new CacheKey(idAndType("countries", "FI"), mutableIncludes, null);
        mutableIncludes.add("languages");

        assertThat(key.getIncludes()).containsExactly("currencies");
    }

    @Test
    void of_typeAndIdOnly_createsKeyWithEmptySets() {
        CacheKey key = CacheKey.of(idAndType("countries", "FI"));

        assertThat(key.getResourceType()).isEqualTo("countries");
        assertThat(key.getResourceId()).isEqualTo("FI");
        assertThat(key.getIncludes()).isEmpty();
        assertThat(key.getFields()).isEmpty();
    }

    @Test
    void of_typeIdAndIncludes_createsKeyWithEmptyFields() {
        CacheKey key = CacheKey.of(idAndType("countries", "FI"), Set.of("currencies"));

        assertThat(key.getResourceType()).isEqualTo("countries");
        assertThat(key.getResourceId()).isEqualTo("FI");
        assertThat(key.getIncludes()).containsExactly("currencies");
        assertThat(key.getFields()).isEmpty();
    }

    @Test
    void equals_sameValues_areEqual() {
        CacheKey key1 = new CacheKey(idAndType("countries", "FI"), Set.of("currencies"), Set.of("name"));
        CacheKey key2 = new CacheKey(idAndType("countries", "FI"), Set.of("currencies"), Set.of("name"));

        assertThat(key1).isEqualTo(key2);
    }

    @Test
    void equals_differentType_areNotEqual() {
        CacheKey key1 = CacheKey.of(idAndType("countries", "FI"));
        CacheKey key2 = CacheKey.of(idAndType("users", "FI"));

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void equals_differentId_areNotEqual() {
        CacheKey key1 = CacheKey.of(idAndType("countries", "FI"));
        CacheKey key2 = CacheKey.of(idAndType("countries", "NO"));

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void equals_differentIncludes_areNotEqual() {
        CacheKey key1 = CacheKey.of(idAndType("countries", "FI"), Set.of("currencies"));
        CacheKey key2 = CacheKey.of(idAndType("countries", "FI"), Set.of("languages"));

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void equals_differentFields_areNotEqual() {
        CacheKey key1 = new CacheKey(idAndType("countries", "FI"), null, Set.of("name"));
        CacheKey key2 = new CacheKey(idAndType("countries", "FI"), null, Set.of("population"));

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void equals_emptyIncludesVsNonEmpty_areNotEqual() {
        CacheKey key1 = CacheKey.of(idAndType("countries", "FI"));
        CacheKey key2 = CacheKey.of(idAndType("countries", "FI"), Set.of("currencies"));

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void hashCode_sameValues_sameHash() {
        CacheKey key1 = new CacheKey(idAndType("countries", "FI"), Set.of("currencies"), Set.of("name"));
        CacheKey key2 = new CacheKey(idAndType("countries", "FI"), Set.of("currencies"), Set.of("name"));

        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    void hashCode_differentValues_differentHash() {
        CacheKey key1 = CacheKey.of(idAndType("countries", "FI"));
        CacheKey key2 = CacheKey.of(idAndType("countries", "NO"));

        assertThat(key1.hashCode()).isNotEqualTo(key2.hashCode());
    }

    @Test
    void toString_containsAllFields() {
        CacheKey key = new CacheKey(idAndType("countries", "FI"), Set.of("currencies"), Set.of("name"));

        String result = key.toString();

        assertThat(result).contains("countries");
        assertThat(result).contains("FI");
        assertThat(result).contains("currencies");
        assertThat(result).contains("name");
    }

    @Test
    void equals_sameQueryParamsInDifferentOrder_equal() {
        Map<String, List<String>> ordered = new LinkedHashMap<>();
        ordered.put("a", List.of("1"));
        ordered.put("b", List.of("2"));
        Map<String, List<String>> reversed = new LinkedHashMap<>();
        reversed.put("b", List.of("2"));
        reversed.put("a", List.of("1"));

        CacheKey key1 = new CacheKey(idAndType("countries", "FI"), null, null, ordered);
        CacheKey key2 = new CacheKey(idAndType("countries", "FI"), null, null, reversed);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.hashCode()).isEqualTo(key2.hashCode());
    }

    @Test
    void equals_differentQueryParamValues_notEqual() {
        CacheKey key1 = new CacheKey(idAndType("countries", "FI"), null, null, Map.of("lang", List.of("en")));
        CacheKey key2 = new CacheKey(idAndType("countries", "FI"), null, null, Map.of("lang", List.of("de")));

        assertThat(key1).isNotEqualTo(key2);
    }

    @Test
    void equals_noQueryParams_equalsKeyWithoutThem() {
        CacheKey key1 = new CacheKey(idAndType("countries", "FI"), null, null, Map.of());
        CacheKey key2 = new CacheKey(idAndType("countries", "FI"), null, null);

        assertThat(key1).isEqualTo(key2);
        assertThat(key1.getQueryParams()).isEmpty();
    }

}
