package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.compound.docs.cache.CacheKey;
import pro.api4.jsonapi4j.compound.docs.cache.CacheResult;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates cache lookup, HTTP fetch for misses, cache storage, and result merging.
 *
 * <p>Sits between {@link pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver} and
 * {@link JsonApi4jCompoundDocsApiHttpClient}. When a cache is configured, it checks
 * the cache before making HTTP calls and stores fetched resources after.
 *
 * <p>When no cache is configured ({@code null}), acts as a pass-through to the HTTP client.
 */
public class CachingCompoundDocsFetcher {

    private final JsonApi4jCompoundDocsApiHttpClient httpClient;
    private final CompoundDocsResourceCache cache;

    /**
     * @param httpClient the HTTP client for downstream fetches, must not be null
     * @param cache      the resource cache, or {@code null} to disable caching
     *                   (fetcher acts as a pass-through to the HTTP client)
     */
    public CachingCompoundDocsFetcher(JsonApi4jCompoundDocsApiHttpClient httpClient,
                                       CompoundDocsResourceCache cache) {
        this.httpClient = Objects.requireNonNull(httpClient, "httpClient must not be null");
        this.cache = cache;
    }

    /**
     * Fetches resources by type and IDs, using the cache when available.
     *
     * <p>When a cache is configured:
     * <ol>
     *   <li>Builds {@link CacheKey} for each requested ID</li>
     *   <li>Checks the cache for all keys via {@link CompoundDocsResourceCache#getAll}</li>
     *   <li>Fetches cache misses via HTTP</li>
     *   <li>Parses the {@code Cache-Control} header from the HTTP response</li>
     *   <li>Stores fetched resources in the cache</li>
     *   <li>Merges cache hits with HTTP results</li>
     * </ol>
     *
     * <p>When no cache is configured, delegates entirely to the HTTP client.
     *
     * @param domainBaseUrl   base URL of the downstream domain
     * @param resourceType    the JSON:API resource type (e.g. {@code "countries"})
     * @param ids             set of resource IDs to fetch
     * @param includes        relationship names for downstream {@code include} parameter
     * @param originalRequest the original compound docs request (for header/field propagation)
     * @param config          resolver configuration
     * @param metaHeaders     metadata headers to add to the downstream request
     * @return the merged fetch result containing all requested resources
     */
    public BatchFetchResult fetch(URI domainBaseUrl,
                                   String resourceType,
                                   Set<String> ids,
                                   Set<String> includes,
                                   CompoundDocsRequest originalRequest,
                                   CompoundDocsResolverConfig config,
                                   Map<String, String> metaHeaders) {
        if (ids == null || ids.isEmpty()) {
            return new BatchFetchResult(Collections.emptyList(), null);
        }

        if (cache == null) {
            return fetchWithoutCache(domainBaseUrl, resourceType, ids, includes,
                    originalRequest, config, metaHeaders);
        }

        return fetchWithCache(domainBaseUrl, resourceType, ids, includes,
                originalRequest, config, metaHeaders);
    }

    private BatchFetchResult fetchWithoutCache(URI domainBaseUrl,
                                                String resourceType,
                                                Set<String> ids,
                                                Set<String> includes,
                                                CompoundDocsRequest originalRequest,
                                                CompoundDocsResolverConfig config,
                                                Map<String, String> metaHeaders) {
        HttpFetchResult httpResult = httpClient.doBatchFetch(
                domainBaseUrl, resourceType, ids, includes, originalRequest, config, metaHeaders);
        List<String> resources = httpResult.resources().stream()
                .map(ParsedResource::json)
                .toList();
        CacheControlDirectives directives = CacheControlParser.parse(httpResult.cacheControlHeader());
        return new BatchFetchResult(resources, directives);
    }

    private BatchFetchResult fetchWithCache(URI domainBaseUrl,
                                             String resourceType,
                                             Set<String> ids,
                                             Set<String> includes,
                                             CompoundDocsRequest originalRequest,
                                             CompoundDocsResolverConfig config,
                                             Map<String, String> metaHeaders) {
        Set<String> fields = resolveFields(resourceType, originalRequest, config);

        // Build CacheKeys for all requested IDs
        Map<CacheKey, String> keyToId = ids.stream()
                .collect(Collectors.toMap(
                        id -> new CacheKey(resourceType, id, includes, fields),
                        id -> id
                ));

        // Cache lookup
        Map<CacheKey, CacheResult> cacheHits = cache.getAll(keyToId.keySet());

        // Collect cache hit JSONs
        List<String> cacheHitJsons = cacheHits.values().stream()
                .map(CacheResult::getResourceJson)
                .toList();

        // Calculate miss IDs
        Set<String> missIds = keyToId.entrySet().stream()
                .filter(e -> !cacheHits.containsKey(e.getKey()))
                .map(Map.Entry::getValue)
                .collect(Collectors.toSet());

        if (missIds.isEmpty()) {
            return new BatchFetchResult(cacheHitJsons, computeDirectives(cacheHits, null));
        }

        // Fetch misses via HTTP
        HttpFetchResult httpResult = httpClient.doBatchFetch(
                domainBaseUrl, resourceType, missIds, includes, originalRequest, config, metaHeaders);

        // Parse Cache-Control and store fetched resources
        CacheControlDirectives directives = CacheControlParser.parse(httpResult.cacheControlHeader());

        List<String> httpResultJsons = new ArrayList<>();
        for (ParsedResource parsed : httpResult.resources()) {
            httpResultJsons.add(parsed.json());
            if (parsed.type() != null && parsed.id() != null) {
                CacheKey key = new CacheKey(resourceType, parsed.id(), includes, fields);
                cache.put(key, parsed.json(), directives);
            }
        }

        // Merge cache hits + HTTP results
        List<String> merged = new ArrayList<>(cacheHitJsons.size() + httpResultJsons.size());
        merged.addAll(cacheHitJsons);
        merged.addAll(httpResultJsons);

        return new BatchFetchResult(merged, computeDirectives(cacheHits, httpResult));
    }

    /**
     * Computes the most restrictive Cache-Control directives from cache hits
     * and an optional HTTP fetch result.
     */
    private CacheControlDirectives computeDirectives(Map<CacheKey, CacheResult> cacheHits,
                                                      HttpFetchResult httpResult) {
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

        if (httpResult != null) {
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
    private Set<String> resolveFields(String resourceType,
                                       CompoundDocsRequest originalRequest,
                                       CompoundDocsResolverConfig config) {
        if (!config.getPropagation().contains(Propagation.FIELDS)) {
            return Collections.emptySet();
        }
        Map<String, List<String>> fieldSets = originalRequest.fieldSets();
        if (fieldSets == null || !fieldSets.containsKey(resourceType)) {
            return Collections.emptySet();
        }
        return new HashSet<>(fieldSets.get(resourceType));
    }
}
