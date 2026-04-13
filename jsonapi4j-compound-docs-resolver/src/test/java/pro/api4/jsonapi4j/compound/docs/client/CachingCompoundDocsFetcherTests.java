package pro.api4.jsonapi4j.compound.docs.client;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.cache.CacheKey;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingCompoundDocsFetcherTests {

    private static final String COUNTRY_FI_JSON = "{\"type\":\"countries\",\"id\":\"FI\",\"attributes\":{\"name\":\"Finland\"}}";
    private static final String COUNTRY_NO_JSON = "{\"type\":\"countries\",\"id\":\"NO\",\"attributes\":{\"name\":\"Norway\"}}";
    private static final String COUNTRY_SE_JSON = "{\"type\":\"countries\",\"id\":\"SE\",\"attributes\":{\"name\":\"Sweden\"}}";

    private static final URI DOMAIN_URL = URI.create("http://localhost");

    @Mock
    private JsonApi4jCompoundDocsApiHttpClient httpClient;

    @Mock
    private CompoundDocsRequest mockRequest;

    @Mock
    private CompoundDocsResolverConfig mockConfig;

    private InMemoryCompoundDocsResourceCache cache;

    private static ParsedResource parsedResource(String type, String id, String json) {
        return new ParsedResource(type, id, json);
    }

    @BeforeEach
    void setUp() {
        cache = new InMemoryCompoundDocsResourceCache(100);
    }

    private void stubConfigNoPropagation() {
        when(mockConfig.getPropagation()).thenReturn(List.of());
    }

    // --- No cache (null) ---

    @Test
    void fetch_nullCache_delegatesToHttpClient() {
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, null);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);
        verify(httpClient).doBatchFetch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void fetch_nullCache_returnsAllHttpResources() {
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON),
                                parsedResource("countries", "NO", COUNTRY_NO_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, null);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);
    }

    // --- All cached ---

    @Test
    void fetch_allCached_noHttpCallMade() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of("countries", "FI");
        CacheKey keyNO = CacheKey.of("countries", "NO");
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));
        cache.put(keyNO, COUNTRY_NO_JSON, CacheControlParser.parse("max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);
        verifyNoInteractions(httpClient);
    }

    @Test
    void fetch_allCached_returnsCachedResources() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of("countries", "FI");
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);
    }

    // --- None cached ---

    @Test
    void fetch_noneCached_fetchesAllViaHttp() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), eq("countries"), eq(Set.of("FI", "NO")),
                any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON),
                                parsedResource("countries", "NO", COUNTRY_NO_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);
        verify(httpClient).doBatchFetch(any(), eq("countries"), eq(Set.of("FI", "NO")),
                any(), any(), any(), any());
    }

    @Test
    void fetch_noneCached_storesResourcesInCache() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        fetcher.fetch(DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        // Verify it's now in cache
        CacheKey keyFI = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyFI)).isPresent();
        assertThat(cache.get(keyFI).get().getResourceJson()).isEqualTo(COUNTRY_FI_JSON);
    }

    @Test
    void fetch_noneCached_parsesCacheControlHeader() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=60"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        fetcher.fetch(DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheKey keyFI = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyFI)).isPresent();
        // TTL should be <= 60 seconds
        assertThat(cache.get(keyFI).get().getRemainingTtlSeconds()).isLessThanOrEqualTo(60);
        assertThat(cache.get(keyFI).get().getRemainingTtlSeconds()).isGreaterThan(0);
    }

    // --- Partial cache hits ---

    @Test
    void fetch_someCached_fetchesOnlyMissesViaHttp() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of("countries", "FI");
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(any(), eq("countries"), eq(Set.of("NO")),
                any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "NO", COUNTRY_NO_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);

        // HTTP client only called for the miss (NO)
        verify(httpClient).doBatchFetch(any(), eq("countries"), eq(Set.of("NO")),
                any(), any(), any(), any());
    }

    @Test
    void fetch_someCached_mergesAllResources() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of("countries", "FI");
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(any(), eq("countries"), eq(Set.of("NO", "SE")),
                any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "NO", COUNTRY_NO_JSON),
                                parsedResource("countries", "SE", COUNTRY_SE_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI", "NO", "SE"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).hasSize(3);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON, COUNTRY_SE_JSON);
    }

    @Test
    void fetch_someCached_storesOnlyHttpResultsInCache() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of("countries", "FI");
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(any(), eq("countries"), eq(Set.of("NO")),
                any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "NO", COUNTRY_NO_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        fetcher.fetch(DOMAIN_URL, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        // NO should now be in cache
        CacheKey keyNO = CacheKey.of("countries", "NO");
        assertThat(cache.get(keyNO)).isPresent();
        assertThat(cache.get(keyNO).get().getResourceJson()).isEqualTo(COUNTRY_NO_JSON);
    }

    // --- Cache-Control handling ---

    @Test
    void fetch_noCacheControlHeader_resourcesNotCached() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        null));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);

        // Should not be cached (null Cache-Control → NON_CACHEABLE)
        CacheKey keyFI = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyFI)).isEmpty();
    }

    @Test
    void fetch_nonCacheableResponse_resourcesNotCached() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "no-store"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        fetcher.fetch(DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheKey keyFI = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyFI)).isEmpty();
    }

    @Test
    void fetch_cacheableResponse_resourcesCached() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        fetcher.fetch(DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheKey keyFI = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyFI)).isPresent();
    }

    // --- CacheKey construction ---

    @Test
    void fetch_cacheKeyIncludesIncludes() {
        stubConfigNoPropagation();
        Set<String> includes = Set.of("regions");

        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        fetcher.fetch(DOMAIN_URL, "countries",
                Set.of("FI"), includes,
                mockRequest, mockConfig, Map.of());

        // Cached with includes in key
        CacheKey keyWithIncludes = CacheKey.of("countries", "FI", includes);
        assertThat(cache.get(keyWithIncludes)).isPresent();

        // Not cached with empty includes
        CacheKey keyWithoutIncludes = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyWithoutIncludes)).isEmpty();
    }

    @Test
    void fetch_fieldsPropagationEnabled_cacheKeyIncludesFields() {
        when(mockConfig.getPropagation()).thenReturn(List.of(Propagation.FIELDS));
        when(mockRequest.fieldSets()).thenReturn(Map.of("countries", List.of("name", "code")));

        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        fetcher.fetch(DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        // Cached with fields in key
        CacheKey keyWithFields = new CacheKey("countries", "FI", Collections.emptySet(), Set.of("name", "code"));
        assertThat(cache.get(keyWithFields)).isPresent();

        // Not cached without fields
        CacheKey keyWithoutFields = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyWithoutFields)).isEmpty();
    }

    @Test
    void fetch_fieldsPropagationDisabled_cacheKeyHasEmptyFields() {
        when(mockConfig.getPropagation()).thenReturn(List.of());

        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        fetcher.fetch(DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        // Cached with empty fields (default key)
        CacheKey keyNoFields = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyNoFields)).isPresent();
    }

    // --- Edge cases ---

    @Test
    void fetch_emptyIds_returnsEmptyResult() {
        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Collections.emptySet(), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    void fetch_nullIds_returnsEmptyResult() {
        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                null, Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    void fetch_httpClientThrows_exceptionPropagates() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        assertThatThrownBy(() -> fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Connection refused");
    }

    @Test
    void fetch_resourceWithNullTypeAndId_stillIncludedInResultsButNotCached() {
        stubConfigNoPropagation();
        String malformedJson = "{\"attributes\":{\"name\":\"Unknown\"}}";
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource(null, null, malformedJson)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        // Resource is still in results
        assertThat(result.resources()).containsExactly(malformedJson);
    }

    @Test
    void constructor_nullHttpClient_throwsNullPointerException() {
        assertThatThrownBy(() -> new CachingCompoundDocsFetcher(null, cache))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("httpClient must not be null");
    }

    // --- Directives — no cache ---

    @Test
    void fetch_nullCache_returnsParsedDirectives() {
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=300"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, null);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.directives()).isNotNull();
        assertThat(result.directives().getMaxAge()).isEqualTo(300L);
    }

    // --- Directives — all cached ---

    @Test
    void fetch_allCached_directivesReflectMinRemainingTtl() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of("countries", "FI");
        CacheKey keyNO = CacheKey.of("countries", "NO");
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));
        cache.put(keyNO, COUNTRY_NO_JSON, CacheControlParser.parse("max-age=60"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.directives()).isNotNull();
        // min TTL should be <= 60 (from the shorter-lived entry)
        assertThat(result.directives().getMaxAge()).isLessThanOrEqualTo(60L);
        assertThat(result.directives().getMaxAge()).isGreaterThan(0L);
    }

    // --- Directives — none cached ---

    @Test
    void fetch_noneCached_directivesFromHttpResponse() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=120, no-cache"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.directives()).isNotNull();
        assertThat(result.directives().getMaxAge()).isEqualTo(120L);
        assertThat(result.directives().isNoCache()).isTrue();
    }

    // --- Directives — partial ---

    @Test
    void fetch_someCached_directivesMergedFromCacheAndHttp() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of("countries", "FI");
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(any(), eq("countries"), eq(Set.of("NO")),
                any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "NO", COUNTRY_NO_JSON)),
                        "max-age=60"));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheControlDirectives directives = result.directives();
        assertThat(directives).isNotNull();
        // Should be min of cache remaining TTL and HTTP max-age=60
        assertThat(directives.getMaxAge()).isLessThanOrEqualTo(60L);
    }

    // --- Directives — no Cache-Control header ---

    @Test
    void fetch_noCacheControlHeader_directivesNonCacheable() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        null));

        var fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_URL, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheControlDirectives directives = result.directives();
        assertThat(directives).isNotNull();
        assertThat(directives.getMaxAge()).isNull();
        assertThat(directives.isNoStore()).isFalse();
    }
}
