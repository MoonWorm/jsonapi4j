package pro.api4.jsonapi4j.compound.docs.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.cache.CacheKey;
import pro.api4.jsonapi4j.compound.docs.cache.CacheResult;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

/**
 * Orchestrates cache lookup, HTTP fetch for misses, cache storage, and result merging.
 *
 * <p>Sits between {@link pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver} and
 * {@link JsonApi4jCompoundDocsApiHttpClient}. When a cache is configured, it checks
 * the cache before making HTTP calls and stores fetched resources after.
 *
 * <p>When the number of cache-miss IDs for a single resource type exceeds
 * {@link DomainSettings#maxBatchSize()}, the misses are split into parallel chunks fetched
 * concurrently via the supplied {@link ExecutorService}. Results and {@code Cache-Control}
 * directives are merged across all chunks.
 *
 * <p>When no cache is configured ({@code null}), acts as a pass-through to the HTTP client
 * (with the same chunking behavior).
 */
@Slf4j
public class CachingCompoundDocsFetcher {

    private final JsonApi4jCompoundDocsApiHttpClient httpClient;
    private final CompoundDocsResourceCache cache;
    private final ExecutorService executorService;

    /**
     * @param httpClient      the HTTP client for downstream fetches, must not be null
     * @param cache           the resource cache, or {@code null} to disable caching
     *                        (fetcher acts as a pass-through to the HTTP client)
     * @param executorService executor used to fan-out chunked HTTP fetches in parallel,
     *                        must not be null
     */
    public CachingCompoundDocsFetcher(JsonApi4jCompoundDocsApiHttpClient httpClient,
                                      CompoundDocsResourceCache cache,
                                      ExecutorService executorService) {
        this.httpClient = Validate.notNull(httpClient, "httpClient must not be null");
        this.executorService = Validate.notNull(executorService, "executorService must not be null");
        this.cache = cache;
    }

    /**
     * Fetches resources by type and IDs, using the cache when available, and splitting
     * downstream HTTP calls into chunks of size {@link DomainSettings#maxBatchSize()} when needed.
     *
     * <p>Flow when a cache is configured:
     * <ol>
     *   <li>Cache lookup runs against the <em>full</em> ID set</li>
     *   <li>Cache-miss IDs are chunked by {@code domainSettings.maxBatchSize()}</li>
     *   <li>Each chunk fires a parallel HTTP request via the executor</li>
     *   <li>Fetched resources are stored back in the cache</li>
     *   <li>Cache hits and HTTP results are merged; {@code Cache-Control} directives are aggregated</li>
     * </ol>
     *
     * <p>Flow without cache: same chunking and parallelism, no cache I/O.
     *
     * @param domainSettings  base URL + max batch size for this resource type, must not be null
     * @param resourceType    the JSON:API resource type (e.g. {@code "countries"})
     * @param ids             set of resource IDs to fetch
     * @param includes        relationship names for downstream {@code include} parameter
     * @param originalRequest the original compound docs request (for header/field propagation)
     * @param config          resolver configuration
     * @param metaHeaders     metadata headers to add to the downstream request
     * @return the merged fetch result containing all requested resources
     */
    public BatchFetchResult fetch(DomainSettings domainSettings,
                                  String resourceType,
                                  Set<String> ids,
                                  Set<String> includes,
                                  CompoundDocsRequest originalRequest,
                                  CompoundDocsResolverConfig config,
                                  Map<String, String> metaHeaders) {
        if (CollectionUtils.isEmpty(ids)) {
            return new BatchFetchResult(Collections.emptyList(), null);
        }

        if (cache == null) {
            return fetchWithoutCache(domainSettings, resourceType, ids, includes,
                    originalRequest, config, metaHeaders);
        }

        return fetchWithCache(domainSettings, resourceType, ids, includes,
                originalRequest, config, metaHeaders);
    }

    private BatchFetchResult fetchWithoutCache(DomainSettings domainSettings,
                                               String resourceType,
                                               Set<String> ids,
                                               Set<String> includes,
                                               CompoundDocsRequest originalRequest,
                                               CompoundDocsResolverConfig config,
                                               Map<String, String> metaHeaders) {
        List<HttpFetchResult> chunkResults = fetchChunksInParallel(
                domainSettings, resourceType, ids, includes,
                originalRequest, config, metaHeaders);

        List<String> resources = new ArrayList<>();
        CacheControlAggregator aggregator = new CacheControlAggregator();
        for (HttpFetchResult chunkResult : chunkResults) {
            for (ParsedResource parsed : chunkResult.resources()) {
                resources.add(parsed.json());
            }
            aggregator.add(CacheControlParser.parse(chunkResult.cacheControlHeader()));
        }
        return new BatchFetchResult(resources, aggregator.getResult());
    }

    private BatchFetchResult fetchWithCache(DomainSettings domainSettings,
                                            String resourceType,
                                            Set<String> ids,
                                            Set<String> includes,
                                            CompoundDocsRequest originalRequest,
                                            CompoundDocsResolverConfig config,
                                            Map<String, String> metaHeaders) {
        Set<String> fields = resolveFieldsQueryParam(resourceType, originalRequest, config);

        // Build CacheKeys for all requested IDs
        Map<CacheKey, String> keyToId = ids.stream()
                .collect(Collectors.toMap(
                        id -> new CacheKey(resourceType, id, includes, fields),
                        id -> id
                ));

        // Cache lookup on the FULL id set
        Map<CacheKey, CacheResult> cacheHits = cache.getAll(keyToId.keySet());

        List<String> cacheHitJsons = cacheHits.values().stream()
                .map(CacheResult::getResourceJson)
                .toList();

        // Calculate miss IDs
        Set<String> missIds = keyToId.entrySet().stream()
                .filter(e -> !cacheHits.containsKey(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        log.debug("Cache lookup for type '{}': {} hits, {} misses", resourceType, cacheHits.size(), missIds.size());

        if (missIds.isEmpty()) {
            log.debug("All resources for type '{}' served from cache", resourceType);
            return new BatchFetchResult(cacheHitJsons, computeDirectives(cacheHits, Collections.emptyList()));
        }

        // Fetch misses in parallel chunks
        List<HttpFetchResult> chunkResults = fetchChunksInParallel(
                domainSettings, resourceType, missIds, includes,
                originalRequest, config, metaHeaders);

        // Store fetched resources in cache (per-chunk Cache-Control governs that chunk's resources)
        List<String> httpResultJsons = new ArrayList<>();
        for (HttpFetchResult chunkResult : chunkResults) {
            CacheControlDirectives chunkDirectives =
                    CacheControlParser.parse(chunkResult.cacheControlHeader());
            for (ParsedResource parsed : chunkResult.resources()) {
                httpResultJsons.add(parsed.json());
                if (parsed.type() != null && parsed.id() != null) {
                    CacheKey key = new CacheKey(resourceType, parsed.id(), includes, fields);
                    cache.put(key, parsed.json(), chunkDirectives);
                }
            }
        }

        // Merge cache hits + HTTP results
        List<String> merged = new ArrayList<>(cacheHitJsons.size() + httpResultJsons.size());
        merged.addAll(cacheHitJsons);
        merged.addAll(httpResultJsons);

        return new BatchFetchResult(merged, computeDirectives(cacheHits, chunkResults));
    }

    /**
     * Splits {@code ids} into chunks of size {@code domainSettings.maxBatchSize()} and fires a
     * downstream HTTP fetch per chunk via the executor. Blocks on
     * {@link CompletableFuture#allOf(CompletableFuture[])} and returns each chunk's result.
     *
     * <p>If a chunk fetch throws, the exception propagates from {@link CompletableFuture#join()} —
     * preserving the same failure semantics as the previous single-call behavior. Under
     * {@link pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy#IGNORE} the HTTP client itself
     * absorbs non-200 responses and returns an empty result, so other chunks continue.
     */
    private List<HttpFetchResult> fetchChunksInParallel(DomainSettings domainSettings,
                                                       String resourceType,
                                                       Set<String> ids,
                                                       Set<String> includes,
                                                       CompoundDocsRequest originalRequest,
                                                       CompoundDocsResolverConfig config,
                                                       Map<String, String> metaHeaders) {
        List<Set<String>> chunks = chunk(ids, domainSettings.maxBatchSize());
        if (chunks.size() == 1) {
            // Fast path: no fan-out needed
            return Collections.singletonList(httpClient.doBatchFetch(
                    domainSettings.url(), resourceType, chunks.getFirst(),
                    includes, originalRequest, config, metaHeaders));
        }

        log.debug("Chunked fetch for type '{}': {} ids → {} chunks (maxBatchSize={})",
                resourceType, ids.size(), chunks.size(), domainSettings.maxBatchSize());

        List<CompletableFuture<HttpFetchResult>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(
                        () -> httpClient.doBatchFetch(
                                domainSettings.url(), resourceType, chunk,
                                includes, originalRequest, config, metaHeaders),
                        executorService))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream().map(CompletableFuture::join).toList();
    }

    /**
     * Splits a set of IDs into ordered chunks of at most {@code chunkSize}.
     * The last chunk may be smaller. Returns an empty list for an empty input.
     */
    static List<Set<String>> chunk(Set<String> ids, int chunkSize) {
        if (ids.isEmpty()) {
            return Collections.emptyList();
        }
        if (ids.size() <= chunkSize) {
            return Collections.singletonList(ids);
        }
        List<String> ordered = new ArrayList<>(ids);
        List<Set<String>> chunks = new ArrayList<>((ordered.size() + chunkSize - 1) / chunkSize);
        for (int i = 0; i < ordered.size(); i += chunkSize) {
            int end = Math.min(i + chunkSize, ordered.size());
            chunks.add(new LinkedHashSet<>(ordered.subList(i, end)));
        }
        return chunks;
    }

    /**
     * Computes the most restrictive {@code Cache-Control} directives from cache hits
     * and zero or more chunk HTTP fetch results.
     */
    private CacheControlDirectives computeDirectives(Map<CacheKey, CacheResult> cacheHits,
                                                     List<HttpFetchResult> httpResults) {
        CacheControlAggregator aggregator = new CacheControlAggregator();

        if (!cacheHits.isEmpty()) {
            long minTtl = cacheHits.values().stream()
                    .mapToLong(CacheResult::getRemainingTtlSeconds)
                    .min()
                    .orElse(0);
            if (minTtl > 0) {
                aggregator.add(CacheControlDirectives.ofMaxAge(minTtl));
            }
        }

        for (HttpFetchResult httpResult : httpResults) {
            aggregator.add(CacheControlParser.parse(httpResult.cacheControlHeader()));
        }

        return aggregator.getResult();
    }

    /**
     * Extracts the sparse fieldset for the given resource type from the original request,
     * if field propagation is enabled.
     *
     * @return the set of field names, or empty set if fields are not propagated
     */
    private Set<String> resolveFieldsQueryParam(String resourceType,
                                                CompoundDocsRequest originalRequest,
                                                CompoundDocsResolverConfig config) {
        if (!config.getPropagation().contains(Propagation.FIELDS)) {
            return Collections.emptySet();
        }
        Map<String, List<String>> fieldSets = originalRequest.getFieldSets();
        if (fieldSets == null || !fieldSets.containsKey(resourceType)) {
            return Collections.emptySet();
        }
        return new HashSet<>(fieldSets.get(resourceType));
    }
}
