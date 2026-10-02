package pro.api4.jsonapi4j.compound.docs.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.IncompleteReason;
import pro.api4.jsonapi4j.compound.docs.cache.CacheKey;
import pro.api4.jsonapi4j.compound.docs.cache.InMemoryCompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.DownstreamTimeoutException;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
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
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static pro.api4.jsonapi4j.compound.docs.IdAndTypeFixtures.idAndType;

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
    private CompoundDocsResolverConfig mockConfig;

    @Mock
    private CompoundDocsRequest mockRequest;

    private InMemoryCompoundDocsResourceCache cache;
    private ExecutorService executor;

    private static ParsedResource parsedResource(String type, String id, String json) {
        return new ParsedResource(type == null || id == null ? null : idAndType(type, id), json);
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
        return new CachingCompoundDocsFetcher(httpClient, cacheArg, executor, mockConfig);
    }

    private static boolean batchOf(BatchFetch batch, Set<String> ids) {
        return batch != null && "countries".equals(batch.resourceType()) && ids.equals(batch.ids());
    }

    private void stubConfigNoPropagation() {
        when(mockConfig.getPropagation()).thenReturn(List.of());
    }

    // --- No cache (null) ---

    @Test
    void fetch_nullCache_delegatesToHttpClient() {
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);
        verify(httpClient).doBatchFetch(any(), any());
    }

    @Test
    void fetch_nullCache_returnsAllHttpResources() {
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON),
                                parsedResource("countries", "NO", COUNTRY_NO_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI", "NO"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);
    }

    // --- All cached ---

    @Test
    void fetch_allCached_noHttpCallMade() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        CacheKey keyNO = CacheKey.of(idAndType("countries", "NO"));
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));
        cache.put(keyNO, COUNTRY_NO_JSON, CacheControlParser.parse("max-age=300"));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI", "NO"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);
        verifyNoInteractions(httpClient);
    }

    @Test
    void fetch_allCached_returnsCachedResources() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);
    }

    // --- None cached ---

    @Test
    void fetch_noneCached_fetchesAllViaHttp() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(argThat(batch -> batchOf(batch, Set.of("FI", "NO"))), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON),
                                parsedResource("countries", "NO", COUNTRY_NO_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI", "NO"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);
        verify(httpClient).doBatchFetch(argThat(batch -> batchOf(batch, Set.of("FI", "NO"))), any());
    }

    @Test
    void fetch_noneCached_storesResourcesInCache() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        assertThat(cache.get(keyFI)).isPresent();
        assertThat(cache.get(keyFI).get().getResourceJson()).isEqualTo(COUNTRY_FI_JSON);
    }

    @Test
    void fetch_noneCached_parsesCacheControlHeader() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=60")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        assertThat(cache.get(keyFI)).isPresent();
        assertThat(cache.get(keyFI).get().getRemainingTtlSeconds()).isLessThanOrEqualTo(60);
        assertThat(cache.get(keyFI).get().getRemainingTtlSeconds()).isGreaterThan(0);
    }

    // --- Partial cache hits ---

    @Test
    void fetch_someCached_fetchesOnlyMissesViaHttp() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(argThat(batch -> batchOf(batch, Set.of("NO"))), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "NO", COUNTRY_NO_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI", "NO"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).hasSize(2);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON);

        verify(httpClient).doBatchFetch(argThat(batch -> batchOf(batch, Set.of("NO"))), any());
    }

    @Test
    void fetch_someCached_mergesAllResources() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(argThat(batch -> batchOf(batch, Set.of("NO", "SE"))), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "NO", COUNTRY_NO_JSON),
                                parsedResource("countries", "SE", COUNTRY_SE_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI", "NO", "SE"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).hasSize(3);
        assertThat(result.resources()).contains(COUNTRY_FI_JSON, COUNTRY_NO_JSON, COUNTRY_SE_JSON);
    }

    @Test
    void fetch_someCached_storesOnlyHttpResultsInCache() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(argThat(batch -> batchOf(batch, Set.of("NO"))), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "NO", COUNTRY_NO_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI", "NO"), Collections.emptySet()), mockRequest);

        CacheKey keyNO = CacheKey.of(idAndType("countries", "NO"));
        assertThat(cache.get(keyNO)).isPresent();
        assertThat(cache.get(keyNO).get().getResourceJson()).isEqualTo(COUNTRY_NO_JSON);
    }

    // --- Cache-Control handling ---

    @Test
    void fetch_noCacheControlHeader_resourcesNotCached() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        null));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);

        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        assertThat(cache.get(keyFI)).isEmpty();
    }

    @Test
    void fetch_nonCacheableResponse_resourcesNotCached() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("no-store")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        assertThat(cache.get(keyFI)).isEmpty();
    }

    @Test
    void fetch_cacheableResponse_resourcesCached() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        assertThat(cache.get(keyFI)).isPresent();
    }

    // --- CacheKey construction ---

    @Test
    void fetch_cacheKeyIncludesIncludes() {
        stubConfigNoPropagation();
        Set<String> includes = Set.of("regions");

        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), includes), mockRequest);

        CacheKey keyWithIncludes = CacheKey.of(idAndType("countries", "FI"), includes);
        assertThat(cache.get(keyWithIncludes)).isPresent();

        CacheKey keyWithoutIncludes = CacheKey.of(idAndType("countries", "FI"));
        assertThat(cache.get(keyWithoutIncludes)).isEmpty();
    }

    @Test
    void fetch_fieldsPropagationEnabled_cacheKeyIncludesFields() {
        when(mockConfig.getPropagation()).thenReturn(List.of(Propagation.FIELDS));
        when(mockRequest.getFieldSets()).thenReturn(Map.of("countries", List.of("name", "code")));

        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        CacheKey keyWithFields = new CacheKey(idAndType("countries", "FI"), Collections.emptySet(), Set.of("name", "code"));
        assertThat(cache.get(keyWithFields)).isPresent();

        CacheKey keyWithoutFields = CacheKey.of(idAndType("countries", "FI"));
        assertThat(cache.get(keyWithoutFields)).isEmpty();
    }

    @Test
    void fetch_fieldsPropagationDisabled_cacheKeyHasEmptyFields() {
        when(mockConfig.getPropagation()).thenReturn(List.of());

        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        CacheKey keyNoFields = CacheKey.of(idAndType("countries", "FI"));
        assertThat(cache.get(keyNoFields)).isPresent();
    }

    @Test
    void fetch_customQueryParamsPropagated_cacheKeyIncludesThem() {
        when(mockConfig.getPropagation()).thenReturn(List.of(Propagation.CUSTOM_QUERY_PARAMS));
        when(mockRequest.getCustomQueryParams()).thenReturn(Map.of("lang", List.of("en")));

        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        CacheKey keyWithParams = new CacheKey(idAndType("countries", "FI"), null, null, Map.of("lang", List.of("en")));
        assertThat(cache.get(keyWithParams)).isPresent();
        assertThat(cache.get(CacheKey.of(idAndType("countries", "FI")))).isEmpty();
    }

    @Test
    void fetch_differentCustomQueryParams_doesNotServeCachedResource() {
        when(mockConfig.getPropagation()).thenReturn(List.of(Propagation.CUSTOM_QUERY_PARAMS));
        when(mockRequest.getCustomQueryParams()).thenReturn(Map.of("lang", List.of("de")));
        cache.put(
                new CacheKey(idAndType("countries", "FI"), null, null, Map.of("lang", List.of("en"))),
                "english-FI",
                CacheControlParser.parse("max-age=300")
        );

        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(
                new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()),
                mockRequest
        );

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);
        verify(httpClient).doBatchFetch(any(), any());
    }

    @Test
    void fetch_customQueryParamsNotPropagated_cacheKeyIgnoresThem() {
        when(mockConfig.getPropagation()).thenReturn(List.of());

        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        assertThat(cache.get(CacheKey.of(idAndType("countries", "FI")))).isPresent();
        verify(mockRequest, never()).getCustomQueryParams();
    }

    @Test
    void fetch_chunkFailsUnderIgnore_skipsItKeepsOtherChunksAndForbidsStoring() {
        when(mockConfig.getErrorStrategy()).thenReturn(ErrorStrategy.IGNORE);
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 1);
        when(httpClient.doBatchFetch(argThat(batch -> batch != null && batch.ids().contains("FI")), any()))
                .thenReturn(new HttpFetchResult(List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), null));
        when(httpClient.doBatchFetch(argThat(batch -> batch != null && batch.ids().contains("NO")), any()))
                .thenThrow(new ErrorJsonApiResponseException("boom"));

        BatchFetchResult result = newFetcher(null).fetch(
                new BatchFetch(settings, "countries", Set.of("FI", "NO"), Collections.emptySet()),
                mockRequest
        );

        assertThat(result.resources()).containsExactly(COUNTRY_FI_JSON);
        assertThat(result.incompleteReason()).isEqualTo(IncompleteReason.FETCH_FAILED);
        assertThat(result.directives().isNoStore()).isTrue();
    }

    @Test
    void fetch_chunkFailsUnderFail_rethrowsItUnwrapped() {
        when(mockConfig.getErrorStrategy()).thenReturn(ErrorStrategy.FAIL);
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 1);
        when(httpClient.doBatchFetch(any(), any())).thenThrow(new DownstreamTimeoutException("slow", null));

        assertThatThrownBy(() -> newFetcher(null).fetch(
                new BatchFetch(settings, "countries", Set.of("FI", "NO"), Collections.emptySet()),
                mockRequest
        )).isInstanceOf(DownstreamTimeoutException.class);
    }

    @Test
    void fetch_allChunksSucceed_hasNoIncompleteReason() {
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), null));

        BatchFetchResult result = newFetcher(null).fetch(
                new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()),
                mockRequest
        );

        assertThat(result.incompleteReason()).isNull();
    }

    // --- Edge cases ---

    @Test
    void fetch_emptyIds_returnsEmptyResult() {
        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Collections.emptySet(), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    void fetch_nullIds_returnsEmptyResult() {
        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", null, Collections.emptySet()), mockRequest);

        assertThat(result.resources()).isEmpty();
        verifyNoInteractions(httpClient);
    }

    @Test
    void fetch_httpClientThrows_exceptionPropagates() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any()))
                .thenThrow(new RuntimeException("Connection refused"));

        var fetcher = newFetcher(cache);

        assertThatThrownBy(() -> fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Connection refused");
    }

    @Test
    void fetch_resourceWithNullTypeAndId_stillIncludedInResultsButNotCached() {
        stubConfigNoPropagation();
        String malformedJson = "{\"attributes\":{\"name\":\"Unknown\"}}";
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource(null, null, malformedJson)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        assertThat(result.resources()).containsExactly(malformedJson);
    }

    @Test
    void constructor_nullHttpClient_throwsNullPointerException() {
        assertThatThrownBy(() -> new CachingCompoundDocsFetcher(null, cache, executor, mockConfig))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("httpClient must not be null");
    }

    @Test
    void constructor_nullConfig_throwsNullPointerException() {
        assertThatThrownBy(() -> new CachingCompoundDocsFetcher(httpClient, cache, executor, null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("config must not be null");
    }

    @Test
    void constructor_nullExecutor_throwsNullPointerException() {
        assertThatThrownBy(() -> new CachingCompoundDocsFetcher(httpClient, cache, null, mockConfig))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("executor must not be null");
    }

    // --- Directives — no cache ---

    @Test
    void fetch_nullCache_returnsParsedDirectives() {
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        assertThat(result.directives()).isNotNull();
        assertThat(result.directives().getMaxAge()).isEqualTo(300L);
    }

    // --- Directives — all cached ---

    @Test
    void fetch_allCached_directivesReflectMinRemainingTtl() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        CacheKey keyNO = CacheKey.of(idAndType("countries", "NO"));
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));
        cache.put(keyNO, COUNTRY_NO_JSON, CacheControlParser.parse("max-age=60"));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI", "NO"), Collections.emptySet()), mockRequest);

        assertThat(result.directives()).isNotNull();
        assertThat(result.directives().getMaxAge()).isLessThanOrEqualTo(60L);
        assertThat(result.directives().getMaxAge()).isGreaterThan(0L);
    }

    // --- Directives — none cached ---

    @Test
    void fetch_noneCached_directivesFromHttpResponse() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)), CacheControlParser.parse("max-age=120, no-cache")));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

        assertThat(result.directives()).isNotNull();
        assertThat(result.directives().getMaxAge()).isEqualTo(120L);
        assertThat(result.directives().isNoCache()).isTrue();
    }

    // --- Directives — partial ---

    @Test
    void fetch_someCached_directivesMergedFromCacheAndHttp() {
        stubConfigNoPropagation();
        CacheKey keyFI = CacheKey.of(idAndType("countries", "FI"));
        cache.put(keyFI, COUNTRY_FI_JSON, CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(argThat(batch -> batchOf(batch, Set.of("NO"))), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "NO", COUNTRY_NO_JSON)), CacheControlParser.parse("max-age=60")));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI", "NO"), Collections.emptySet()), mockRequest);

        CacheControlDirectives directives = result.directives();
        assertThat(directives).isNotNull();
        assertThat(directives.getMaxAge()).isLessThanOrEqualTo(60L);
    }

    // --- Directives — no Cache-Control header ---

    @Test
    void fetch_noCacheControlHeader_directivesNonCacheable() {
        stubConfigNoPropagation();
        when(httpClient.doBatchFetch(any(), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "FI", COUNTRY_FI_JSON)),
                        null));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(DOMAIN_SETTINGS, "countries", Set.of("FI"), Collections.emptySet()), mockRequest);

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

        when(httpClient.doBatchFetch(argThat(batch -> batchOf(batch, ids)), any()))
                .thenReturn(new HttpFetchResult(
                        List.of(parsedResource("countries", "A", "{\"id\":\"A\"}"),
                                parsedResource("countries", "B", "{\"id\":\"B\"}"),
                                parsedResource("countries", "C", "{\"id\":\"C\"}")), CacheControlParser.parse("max-age=300")));

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(settings, "countries", ids, Collections.emptySet()), mockRequest);

        assertThat(result.resources()).hasSize(3);
        verify(httpClient, times(1)).doBatchFetch(any(), any());
    }

    @Test
    void fetch_idsAboveBatchSize_splitsIntoParallelChunks() {
        stubConfigNoPropagation();
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);
        Set<String> ids = Set.of("A", "B", "C", "D", "E");

        // Any chunk of size <= 2 returns its inputs as parsed resources
        when(httpClient.doBatchFetch(argThat(batch -> "countries".equals(batch.resourceType()) && batch.ids().size() <= 2), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = ((BatchFetch) inv.getArgument(0)).ids();
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "{\"id\":\"" + id + "\"}"))
                            .toList();
                    return new HttpFetchResult(resources, CacheControlParser.parse("max-age=300"));
                });

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(settings, "countries", ids, Collections.emptySet()), mockRequest);

        // 5 ids / 2 per chunk = 3 chunks (ceil)
        verify(httpClient, times(3)).doBatchFetch(any(), any());
        assertThat(result.resources()).hasSize(5);
    }

    @Test
    void fetch_idsAboveBatchSize_allResourcesMergedAcrossChunks() {
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);
        Set<String> ids = Set.of("A", "B", "C", "D");

        when(httpClient.doBatchFetch(any(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = ((BatchFetch) inv.getArgument(0)).ids();
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "json-" + id))
                            .toList();
                    return new HttpFetchResult(resources, CacheControlParser.parse("max-age=300"));
                });

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(settings, "countries", ids, Collections.emptySet()), mockRequest);

        assertThat(result.resources()).containsExactlyInAnyOrder("json-A", "json-B", "json-C", "json-D");
    }

    @Test
    void fetch_cacheLookupOnFullSet_thenChunkMisses() {
        stubConfigNoPropagation();
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);

        // Pre-cache one resource → misses are A, C, D, E (4 ids → 2 chunks of 2)
        cache.put(CacheKey.of(idAndType("countries", "B")), "cached-B", CacheControlParser.parse("max-age=300"));

        when(httpClient.doBatchFetch(argThat(batch -> "countries".equals(batch.resourceType()) && batch.ids().size() <= 2), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = ((BatchFetch) inv.getArgument(0)).ids();
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "fetched-" + id))
                            .toList();
                    return new HttpFetchResult(resources, CacheControlParser.parse("max-age=300"));
                });

        var fetcher = newFetcher(cache);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(settings, "countries", Set.of("A", "B", "C", "D", "E"), Collections.emptySet()), mockRequest);

        // 4 misses (A,C,D,E) split into 2 chunks of 2
        verify(httpClient, times(2)).doBatchFetch(any(), any());
        assertThat(result.resources()).hasSize(5);
        assertThat(result.resources()).contains("cached-B", "fetched-A", "fetched-C", "fetched-D", "fetched-E");
    }

    @Test
    void fetch_chunkedFetch_directivesAggregatedAcrossChunks() {
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);
        Set<String> ids = Set.of("A", "B", "C", "D");

        // Different chunks return different Cache-Control directives → aggregator picks the most restrictive
        when(httpClient.doBatchFetch(any(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = ((BatchFetch) inv.getArgument(0)).ids();
                    String header = chunkIds.contains("A") ? "max-age=300" : "max-age=30";
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "json-" + id))
                            .toList();
                    return new HttpFetchResult(resources, CacheControlParser.parse(header));
                });

        var fetcher = newFetcher(null);

        BatchFetchResult result = fetcher.fetch(new BatchFetch(settings, "countries", ids, Collections.emptySet()), mockRequest);

        assertThat(result.directives()).isNotNull();
        assertThat(result.directives().getMaxAge()).isEqualTo(30L);
    }

    @Test
    void fetch_chunkedFetch_storesAllResultsInCache() {
        stubConfigNoPropagation();
        DomainSettings settings = new DomainSettings(DOMAIN_URL, 2);
        Set<String> ids = Set.of("A", "B", "C");

        when(httpClient.doBatchFetch(any(), any()))
                .thenAnswer(inv -> {
                    @SuppressWarnings("unchecked")
                    Set<String> chunkIds = ((BatchFetch) inv.getArgument(0)).ids();
                    List<ParsedResource> resources = chunkIds.stream()
                            .map(id -> parsedResource("countries", id, "json-" + id))
                            .toList();
                    return new HttpFetchResult(resources, CacheControlParser.parse("max-age=300"));
                });

        var fetcher = newFetcher(cache);

        fetcher.fetch(new BatchFetch(settings, "countries", ids, Collections.emptySet()), mockRequest);

        for (String id : ids) {
            assertThat(cache.get(CacheKey.of(idAndType("countries", id)))).isPresent();
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
