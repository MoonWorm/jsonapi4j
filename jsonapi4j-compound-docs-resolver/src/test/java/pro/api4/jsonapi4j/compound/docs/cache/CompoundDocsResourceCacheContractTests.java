package pro.api4.jsonapi4j.compound.docs.cache;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CompoundDocsResourceCacheContractTests {

    private static final CacheControlDirectives CACHEABLE_DIRECTIVES =
            CacheControlParser.parse("max-age=300");

    private TestCache cache;

    @BeforeEach
    void setUp() {
        cache = new TestCache(300);
    }

    // --- get / put basics ---

    @Test
    void put_thenGet_returnsCachedResult() {
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, "{\"type\":\"countries\",\"id\":\"FI\"}", CACHEABLE_DIRECTIVES);

        Optional<CacheResult> result = cache.get(key);
        assertThat(result).isPresent();
        assertThat(result.get().getResourceJson()).isEqualTo("{\"type\":\"countries\",\"id\":\"FI\"}");
        assertThat(result.get().getRemainingTtlSeconds()).isEqualTo(300);
    }

    @Test
    void get_missingKey_returnsEmpty() {
        Optional<CacheResult> result = cache.get(CacheKey.of("countries", "FI"));

        assertThat(result).isEmpty();
    }

    @Test
    void put_differentKeys_storedIndependently() {
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_DIRECTIVES);
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_DIRECTIVES);

        assertThat(cache.get(key1).get().getResourceJson()).isEqualTo("{\"id\":\"FI\"}");
        assertThat(cache.get(key2).get().getResourceJson()).isEqualTo("{\"id\":\"NO\"}");
    }

    @Test
    void put_sameKeyTwice_overwritesPrevious() {
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, "{\"version\":1}", CACHEABLE_DIRECTIVES);
        cache.put(key, "{\"version\":2}", CACHEABLE_DIRECTIVES);

        assertThat(cache.get(key).get().getResourceJson()).isEqualTo("{\"version\":2}");
    }

    // --- isCacheable guard (AbstractCompoundDocsResourceCache) ---

    @Test
    void put_nonCacheableNoStore_doesNotStore() {
        CacheKey key = CacheKey.of("countries", "FI");
        CacheControlDirectives directives = CacheControlParser.parse("no-store");

        cache.put(key, "{}", directives);

        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void put_nonCacheableNoCache_doesNotStore() {
        CacheKey key = CacheKey.of("countries", "FI");
        CacheControlDirectives directives = CacheControlParser.parse("no-cache");

        cache.put(key, "{}", directives);

        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void put_nonCacheablePrivate_doesNotStore() {
        CacheKey key = CacheKey.of("countries", "FI");
        CacheControlDirectives directives = CacheControlParser.parse("private, max-age=300");

        cache.put(key, "{}", directives);

        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void put_nonCacheableNullTtl_doesNotStore() {
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, "{}", CacheControlDirectives.NON_CACHEABLE);

        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void put_cacheableDirectives_stores() {
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, "{\"id\":\"FI\"}", CACHEABLE_DIRECTIVES);

        assertThat(cache.get(key)).isPresent();
    }

    // --- CacheKey matching ---

    @Test
    void get_differentIncludesForSameTypeAndId_returnsDifferentResults() {
        CacheKey keyWithCurrencies = CacheKey.of("countries", "FI", Set.of("currencies"));
        CacheKey keyWithLanguages = CacheKey.of("countries", "FI", Set.of("languages"));

        cache.put(keyWithCurrencies, "{\"includes\":\"currencies\"}", CACHEABLE_DIRECTIVES);
        cache.put(keyWithLanguages, "{\"includes\":\"languages\"}", CACHEABLE_DIRECTIVES);

        assertThat(cache.get(keyWithCurrencies).get().getResourceJson())
                .isEqualTo("{\"includes\":\"currencies\"}");
        assertThat(cache.get(keyWithLanguages).get().getResourceJson())
                .isEqualTo("{\"includes\":\"languages\"}");
    }

    @Test
    void get_differentFieldsForSameTypeAndId_returnsDifferentResults() {
        CacheKey keyWithName = new CacheKey("countries", "FI", null, Set.of("name"));
        CacheKey keyWithPopulation = new CacheKey("countries", "FI", null, Set.of("population"));

        cache.put(keyWithName, "{\"fields\":\"name\"}", CACHEABLE_DIRECTIVES);
        cache.put(keyWithPopulation, "{\"fields\":\"population\"}", CACHEABLE_DIRECTIVES);

        assertThat(cache.get(keyWithName).get().getResourceJson())
                .isEqualTo("{\"fields\":\"name\"}");
        assertThat(cache.get(keyWithPopulation).get().getResourceJson())
                .isEqualTo("{\"fields\":\"population\"}");
    }

    @Test
    void get_sameKeyWithIncludesInDifferentOrder_returnsSameResult() {
        CacheKey key1 = CacheKey.of("countries", "FI", Set.of("currencies", "languages"));
        CacheKey key2 = CacheKey.of("countries", "FI", Set.of("languages", "currencies"));

        cache.put(key1, "{\"data\":\"test\"}", CACHEABLE_DIRECTIVES);

        assertThat(cache.get(key2)).isPresent();
        assertThat(cache.get(key2).get().getResourceJson()).isEqualTo("{\"data\":\"test\"}");
    }

    // --- Bulk operations (default methods) ---

    @Test
    void getAll_mixOfHitsAndMisses_returnsOnlyHits() {
        CacheKey hit1 = CacheKey.of("countries", "FI");
        CacheKey hit2 = CacheKey.of("countries", "NO");
        CacheKey miss = CacheKey.of("countries", "SE");

        cache.put(hit1, "{\"id\":\"FI\"}", CACHEABLE_DIRECTIVES);
        cache.put(hit2, "{\"id\":\"NO\"}", CACHEABLE_DIRECTIVES);

        Map<CacheKey, CacheResult> results = cache.getAll(List.of(hit1, hit2, miss));

        assertThat(results).hasSize(2);
        assertThat(results).containsKey(hit1);
        assertThat(results).containsKey(hit2);
        assertThat(results).doesNotContainKey(miss);
    }

    @Test
    void getAll_emptyCollection_returnsEmptyMap() {
        Map<CacheKey, CacheResult> results = cache.getAll(List.of());

        assertThat(results).isEmpty();
    }

    @Test
    void getAll_allMisses_returnsEmptyMap() {
        Map<CacheKey, CacheResult> results = cache.getAll(
                List.of(CacheKey.of("countries", "FI"), CacheKey.of("countries", "NO")));

        assertThat(results).isEmpty();
    }

    @Test
    void putAll_storesAllEntries() {
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");
        Map<CacheKey, String> resources = Map.of(
                key1, "{\"id\":\"FI\"}",
                key2, "{\"id\":\"NO\"}");

        cache.putAll(resources, CACHEABLE_DIRECTIVES);

        assertThat(cache.get(key1)).isPresent();
        assertThat(cache.get(key2)).isPresent();
    }

    @Test
    void putAll_nonCacheableDirectives_storesNothing() {
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");
        Map<CacheKey, String> resources = Map.of(
                key1, "{\"id\":\"FI\"}",
                key2, "{\"id\":\"NO\"}");

        cache.putAll(resources, CacheControlDirectives.NON_CACHEABLE);

        assertThat(cache.get(key1)).isEmpty();
        assertThat(cache.get(key2)).isEmpty();
    }

    // --- Test implementation ---

    /**
     * Minimal implementation for testing the SPI contract.
     * Stores entries in a HashMap with no TTL tracking — just stores and retrieves.
     * Uses a fixed remaining TTL for simplicity.
     */
    private static class TestCache extends AbstractCompoundDocsResourceCache {

        private final Map<CacheKey, String> store = new HashMap<>();
        private final long fixedRemainingTtl;

        TestCache(long fixedRemainingTtl) {
            this.fixedRemainingTtl = fixedRemainingTtl;
        }

        @Override
        public Optional<CacheResult> get(CacheKey key) {
            String json = store.get(key);
            return json != null
                    ? Optional.of(new CacheResult(json, fixedRemainingTtl))
                    : Optional.empty();
        }

        @Override
        protected void doPut(CacheKey key, String resourceJson, CacheControlDirectives directives) {
            store.put(key, resourceJson);
        }
    }

}
