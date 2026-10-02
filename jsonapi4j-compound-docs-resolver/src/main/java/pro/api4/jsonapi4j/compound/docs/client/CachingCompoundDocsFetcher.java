package pro.api4.jsonapi4j.compound.docs.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.IncompleteReason;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.cache.CacheKey;
import pro.api4.jsonapi4j.compound.docs.cache.CacheResult;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
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
 * concurrently via the supplied {@link Executor}. Results and {@code Cache-Control}
 * directives are merged across all chunks.
 *
 * <p>When no cache is configured ({@code null}), acts as a pass-through to the HTTP client
 * (with the same chunking behavior).
 */
@Slf4j
public class CachingCompoundDocsFetcher {

    private final JsonApi4jCompoundDocsApiHttpClient httpClient;
    private final CompoundDocsResourceCache cache;
    private final Executor executor;
    private final CompoundDocsResolverConfig config;

    /**
     * @param httpClient      the HTTP client for downstream fetches, must not be null
     * @param cache           the resource cache, or {@code null} to disable caching
     *                        (fetcher acts as a pass-through to the HTTP client)
     * @param executor executor used to fan-out chunked HTTP fetches in parallel,
     *                        must not be null
     * @param config          resolver configuration, must not be null
     */
    public CachingCompoundDocsFetcher(JsonApi4jCompoundDocsApiHttpClient httpClient,
                                      CompoundDocsResourceCache cache,
                                      Executor executor,
                                      CompoundDocsResolverConfig config) {
        this.httpClient = Validate.notNull(httpClient, "httpClient must not be null");
        this.executor = Validate.notNull(executor, "executor must not be null");
        this.config = Validate.notNull(config, "config must not be null");
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
     * @param batch           the resources to fetch and where from
     * @param originalRequest the original compound docs request (for header/field propagation)
     * @return the merged fetch result containing all requested resources
     */
    public BatchFetchResult fetch(BatchFetch batch, CompoundDocsRequest originalRequest) {
        if (CollectionUtils.isEmpty(batch.ids())) {
            return new BatchFetchResult(Collections.emptyList(), null);
        }

        if (cache == null) {
            return fetchWithoutCache(batch, originalRequest);
        }

        return fetchWithCache(batch, originalRequest);
    }

    private BatchFetchResult fetchWithoutCache(BatchFetch batch, CompoundDocsRequest originalRequest) {
        List<HttpFetchResult> chunkResults = fetchChunksInParallel(batch, originalRequest);

        List<String> resources = new ArrayList<>();
        CacheControlAggregator aggregator = new CacheControlAggregator();
        for (HttpFetchResult chunkResult : chunkResults) {
            for (ParsedResource parsed : chunkResult.resources()) {
                resources.add(parsed.json());
            }
            aggregator.add(chunkResult.directives());
        }
        return new BatchFetchResult(resources, aggregator.getResult(), incompleteReason(chunkResults));
    }

    private BatchFetchResult fetchWithCache(BatchFetch batch, CompoundDocsRequest originalRequest) {
        String resourceType = batch.resourceType();
        Set<String> includes = batch.includes();
        Set<String> fields = resolveFieldsQueryParam(resourceType, originalRequest);
        Map<String, List<String>> queryParams = resolveCustomQueryParams(originalRequest);
        ResourceType type = new ResourceType(resourceType);

        // Build CacheKeys for all requested IDs
        Set<CacheKey> keys = batch.ids().stream()
                .map(id -> new CacheKey(new IdAndType(id, type), includes, fields, queryParams))
                .collect(Collectors.toSet());

        // Cache lookup on the FULL id set
        Map<CacheKey, CacheResult> cacheHits = cache.getAll(keys);

        List<String> cacheHitJsons = cacheHits.values().stream()
                .map(CacheResult::getResourceJson)
                .toList();

        // Calculate miss IDs
        Set<String> missIds = keys.stream()
                .filter(key -> !cacheHits.containsKey(key))
                .map(CacheKey::getResourceId)
                .collect(Collectors.toSet());

        log.debug("Cache lookup for type '{}': {} hits, {} misses", resourceType, cacheHits.size(), missIds.size());

        if (missIds.isEmpty()) {
            log.debug("All resources for type '{}' served from cache", resourceType);
            return new BatchFetchResult(cacheHitJsons, computeDirectives(cacheHits, Collections.emptyList()));
        }

        // Fetch misses in parallel chunks
        List<HttpFetchResult> chunkResults = fetchChunksInParallel(batch.withIds(missIds), originalRequest);

        // Store fetched resources in cache (per-chunk Cache-Control governs that chunk's resources)
        List<String> httpResultJsons = new ArrayList<>();
        for (HttpFetchResult chunkResult : chunkResults) {
            CacheControlDirectives chunkDirectives = chunkResult.directives();
            for (ParsedResource parsed : chunkResult.resources()) {
                httpResultJsons.add(parsed.json());
                if (parsed.idAndType() != null) {
                    CacheKey key = new CacheKey(
                            new IdAndType(parsed.idAndType().getId(), type),
                            includes,
                            fields,
                            queryParams
                    );
                    cache.put(key, parsed.json(), chunkDirectives);
                }
            }
        }

        // Merge cache hits + HTTP results
        List<String> merged = new ArrayList<>(cacheHitJsons.size() + httpResultJsons.size());
        merged.addAll(cacheHitJsons);
        merged.addAll(httpResultJsons);

        return new BatchFetchResult(merged, computeDirectives(cacheHits, chunkResults), incompleteReason(chunkResults));
    }

    /**
     * Splits the batch's ids into chunks of size {@code domainSettings.maxBatchSize()} and fires a
     * downstream HTTP fetch per chunk via the executor. Blocks on
     * {@link CompletableFuture#allOf(CompletableFuture[])} and returns each chunk's result.
     *
     * <p>A failed chunk is ignored under {@link ErrorStrategy#IGNORE} - it contributes no resources and is marked
     * {@link HttpFetchResult#failed()}, while the other chunks continue. Under {@link ErrorStrategy#FAIL} its exception
     * propagates as thrown, unwrapped from the {@link CompletionException}.
     */
    private List<HttpFetchResult> fetchChunksInParallel(BatchFetch batch, CompoundDocsRequest originalRequest) {
        int maxBatchSize = batch.domainSettings().maxBatchSize();
        List<Set<String>> chunks = chunk(batch.ids(), maxBatchSize);
        if (chunks.size() == 1) {
            // Fast path: no fan-out needed
            return Collections.singletonList(fetchChunk(batch.withIds(chunks.get(0)), originalRequest));
        }

        log.debug("Chunked fetch for type '{}': {} ids → {} chunks (maxBatchSize={})",
                batch.resourceType(), batch.ids().size(), chunks.size(), maxBatchSize);

        List<CompletableFuture<HttpFetchResult>> futures = chunks.stream()
                .map(chunk -> CompletableFuture.supplyAsync(
                        () -> fetchChunk(batch.withIds(chunk), originalRequest),
                        executor))
                .toList();

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }

        return futures.stream().map(CompletableFuture::join).toList();
    }

    private HttpFetchResult fetchChunk(BatchFetch chunk, CompoundDocsRequest originalRequest) {
        try {
            return httpClient.doBatchFetch(chunk, originalRequest);
        } catch (ErrorJsonApiResponseException e) {
            if (config.getErrorStrategy() == ErrorStrategy.IGNORE) {
                log.warn(
                        "Ignoring a failed fetch of '{}' resources {} per error strategy: {}",
                        chunk.resourceType(),
                        chunk.ids(),
                        e.getMessage()
                );
                return HttpFetchResult.ignoredFailure();
            }
            throw e;
        }
    }

    private IncompleteReason incompleteReason(List<HttpFetchResult> chunkResults) {
        return chunkResults.stream().anyMatch(HttpFetchResult::failed) ? IncompleteReason.FETCH_FAILED : null;
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
            aggregator.add(httpResult.directives());
        }

        return aggregator.getResult();
    }

    /**
     * The custom query parameters sent downstream, if they are propagated - a downstream response may vary by them,
     * so they are part of the cache key.
     *
     * @return the propagated custom query parameters, or an empty map if they are not propagated
     */
    private Map<String, List<String>> resolveCustomQueryParams(CompoundDocsRequest originalRequest) {
        if (!config.getPropagation().contains(Propagation.CUSTOM_QUERY_PARAMS)) {
            return Collections.emptyMap();
        }
        return originalRequest.getCustomQueryParams();
    }

    /**
     * Extracts the sparse fieldset for the given resource type from the original request,
     * if field propagation is enabled.
     *
     * @return the set of field names, or empty set if fields are not propagated
     */
    private Set<String> resolveFieldsQueryParam(String resourceType, CompoundDocsRequest originalRequest) {
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
