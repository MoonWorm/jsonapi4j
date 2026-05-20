package pro.api4.jsonapi4j.compound.docs.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CachingCompoundDocsFetcherTests {

    private static final String COUNTRY_FI_JSON = "{\"type\":\"countries\",\"id\":\"FI\",\"attributes\":{\"name\":\"Finland\"}}";
    private static final String COUNTRY_NO_JSON = "{\"type\":\"countries\",\"id\":\"NO\",\"attributes\":{\"name\":\"Norway\"}}";
    private static final String COUNTRY_SE_JSON = "{\"type\":\"countries\",\"id\":\"SE\",\"attributes\":{\"name\":\"Sweden\"}}";

    private static final URI DOMAIN_URL = URI.create("http://localhost");
    private static final DomainSettings DOMAIN_SETTINGS = DomainSettings.of(DOMAIN_URL);

    @Mock
    private JsonApi4jCompoundDocsApiHttpClient httpClient;

    @Mock
    private CompoundDocsRequest mockRequest;

    @Mock
    private CompoundDocsResolverConfig mockConfig;

    private InMemoryCompoundDocsResourceCache cache;
    private ExecutorService executor;

    private static ParsedResource parsedResource(String type, String id, String json) {
        return new ParsedResource(type, id, json);
    }

    @BeforeEach
    void setUp() {
        cache = new InMemoryCompoundDocsResourceCache(100);
        executor = Executors.newCachedThreadPool();
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    private CachingCompoundDocsFetcher newFetcher(InMemoryCompoundDocsResourceCache cacheArg) {
        return new CachingCompoundDocsFetcher(httpClient, cacheArg, executor);
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

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        fetcher.fetch(DOMAIN_SETTINGS, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

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

        var fetcher = newFetcher(cache);

        fetcher.fetch(DOMAIN_SETTINGS, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheKey keyFI = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyFI)).isPresent();
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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);

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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        fetcher.fetch(DOMAIN_SETTINGS, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);

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

        var fetcher = newFetcher(cache);

        fetcher.fetch(DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        fetcher.fetch(DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        fetcher.fetch(DOMAIN_SETTINGS, "countries",
                Set.of("FI"), includes,
                mockRequest, mockConfig, Map.of());

        CacheKey keyWithIncludes = CacheKey.of("countries", "FI", includes);
        assertThat(cache.get(keyWithIncludes)).isPresent();

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

        var fetcher = newFetcher(cache);

        fetcher.fetch(DOMAIN_SETTINGS, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheKey keyWithFields = new CacheKey("countries", "FI", Collections.emptySet(), Set.of("name", "code"));
        assertThat(cache.get(keyWithFields)).isPresent();

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

        var fetcher = newFetcher(cache);

        fetcher.fetch(DOMAIN_SETTINGS, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheKey keyNoFields = CacheKey.of("countries", "FI");
        assertThat(cache.get(keyNoFields)).isPresent();
    }

    // --- Edge cases ---

    @Test
    void fetch_emptyIds_returnsEmptyResult() {
        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
                Collections.emptySet(), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    void fetch_nullIds_returnsEmptyResult() {
        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        assertThatThrownBy(() -> fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection refused");
    }

    @Test
    void fetch_resourceWithNullTypeAndId_stillIncludedInResultsButNotCached() {
        stubConfigNoPropagation();
        String malformedJson = "{\"attributes\":{\"name\":\"Unknown\"}}";
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource(null, null, malformedJson)),
                        "max-age=300"));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).containsExactly(malformedJson);
    }

    @Test
    void constructor_nullHttpClient_throwsNullPointerException() {
        assertThatThrownBy(() -> new CachingCompoundDocsFetcher(null, cache, executor))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("httpClient must not be null");
    }

    @Test
    void constructor_nullExecutor_throwsNullPointerException() {
        assertThatThrownBy(() -> new CachingCompoundDocsFetcher(httpClient, cache, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("executorService must not be null");
    }

    // --- Directives — no cache ---

    @Test
    void fetch_nullCache_returnsParsedDirectives() {
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        "max-age=300"));

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.directives()).isNotNull();
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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
                Set.of("FI", "NO"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheControlDirectives directives = result.directives();
        assertThat(directives).isNotNull();
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

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                DOMAIN_SETTINGS, "countries",
                Set.of("FI"), Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        CacheControlDirectives directives = result.directives();
        assertThat(directives).isNotNull();
        assertThat(directives.getMaxAge()).isNull();
        assertThat(directives.isNoStore()).isFalse();
    }

    // --- Chunking ---

    @Test
    void fetch_idsAtBatchSize_singleHttpCall() {
        stubConfigNoPropagation();
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 3);
        Set<String> ids = Set.of("A", "B", "C");

        when(httpClient.doBatchFetch(any(), eq("countries"), eq(ids),
                any(), any(), any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "A", "{\"id\":\"A\"}"),
                                parsedResource("countries", "B", "{\"id\":\"B\"}"),
                                parsedResource("countries", "C", "{\"id\":\"C\"}")),
                        "max-age=300"));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                settings, "countries", ids, Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).hasSize(3);
        verify(httpClient, times(1)).doBatchFetch(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void fetch_idsAboveBatchSize_splitsIntoParallelChunks() {
        stubConfigNoPropagation();
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);
        Set<String> ids = Set.of("A", "B", "C", "D", "E");

        // Any chunk of size <= 2 returns its inputs as parsed resources
        when(httpClient.doBatchFetch(any(), eq("countries"), argThat(chunk -> chunk != null && chunk.size() <= 2),
                any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = (Set<String>) inv.getArgument(2);
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "{\"id\":\"" + id + "\"}"))
                            .toList();
                    return new HttpFetchResult(resources, "max-age=300");
                });

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                settings, "countries", ids, Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        // 5 ids / 2 per chunk = 3 chunks (ceil)
        verify(httpClient, times(3)).doBatchFetch(any(), any(), any(), any(), any(), any(), any());
        assertThat(result.resources()).hasSize(5);
    }

    @Test
    void fetch_idsAboveBatchSize_allResourcesMergedAcrossChunks() {
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);
        Set<String> ids = Set.of("A", "B", "C", "D");

        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = (Set<String>) inv.getArgument(2);
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "json-" + id))
                            .toList();
                    return new HttpFetchResult(resources, "max-age=300");
                });

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(
                settings, "countries", ids, Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.resources()).containsExactlyInAnyOrder("json-A", "json-B", "json-C", "json-D");
    }

    @Test
    void fetch_cacheLookupOnFullSet_thenChunkMisses() {
        stubConfigNoPropagation();
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);

        // Pre-cache one resource → misses are A, C, D, E (4 ids → 2 chunks of 2)
        cache.put(CacheKey.of("countries", "B"), "cached-B", CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(any(), eq("countries"), argThat(chunk -> chunk != null && chunk.size() <= 2),
                any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = (Set<String>) inv.getArgument(2);
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "fetched-" + id))
                            .toList();
                    return new HttpFetchResult(resources, "max-age=300");
                });

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                settings, "countries", Set.of("A", "B", "C", "D", "E"),
                Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        // 4 misses (A,C,D,E) split into 2 chunks of 2
        verify(httpClient, times(2)).doBatchFetch(any(), any(), any(), any(), any(), any(), any());
        assertThat(result.resources()).hasSize(5);
        assertThat(result.resources()).contains("cached-B", "fetched-A", "fetched-C", "fetched-D", "fetched-E");
    }

    @Test
    void fetch_chunkedFetch_directivesAggregatedAcrossChunks() {
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);
        Set<String> ids = Set.of("A", "B", "C", "D");

        // Different chunks return different Cache-Control directives → aggregator picks the most restrictive
        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = (Set<String>) inv.getArgument(2);
                    String header = chunkIds.contains("A") ? "max-age=300" : "max-age=30";
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "json-" + id))
                            .toList();
                    return new HttpFetchResult(resources, header);
                });

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(
                settings, "countries", ids, Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        assertThat(result.directives()).isNotNull();
        assertThat(result.directives().getMaxAge()).isEqualTo(30L);
    }

    @Test
    void fetch_chunkedFetch_storesAllResultsInCache() {
        stubConfigNoPropagation();
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);
        Set<String> ids = Set.of("A", "B", "C");

        when(httpClient.doBatchFetch(any(), any(), any(), any(), any(), any(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = (Set<String>) inv.getArgument(2);
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "json-" + id))
                            .toList();
                    return new HttpFetchResult(resources, "max-age=300");
                });

        var fetcher = newFetcher(cache);

        fetcher.fetch(settings, "countries", ids, Collections.emptySet(),
                mockRequest, mockConfig, Map.of());

        for (String id : ids) {
            assertThat(cache.get(CacheKey.of("countries", id))).isPresent();
        }
    }

    @Test
    void chunkHelper_splitsCorrectly() {
        assertThat(CachingCompoundDocsFetcher.chunk(Collections.emptySet(), 5)).isEmpty();
        assertThat(CachingCompoundDocsFetcher.chunk(Set.of("A"), 5)).hasSize(1);
        assertThat(CachingCompoundDocsFetcher.chunk(Set.of("A", "B", "C"), 3)).hasSize(1);
        assertThat(CachingCompoundDocsFetcher.chunk(Set.of("A", "B", "C", "D"), 2)).hasSize(2);
        assertThat(CachingCompoundDocsFetcher.chunk(Set.of("A", "B", "C", "D", "E"), 2)).hasSize(3);
        assertThat(CachingCompoundDocsFetcher.chunk(Set.of("A", "B", "C", "D", "E", "F", "G"), 3)).hasSize(3);
    }
}
