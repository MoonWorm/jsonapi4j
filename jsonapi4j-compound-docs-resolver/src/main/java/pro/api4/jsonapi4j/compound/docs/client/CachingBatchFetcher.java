package pro.api4.jsonapi4j.compound.docs.client;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.cache.CacheKey;
import pro.api4.jsonapi4j.compound.docs.cache.CacheResult;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.compound.docs.json.ParsedResource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Serves resources from the cache, fetching only the misses - in one fetch for the whole set of misses, so the
 * delegate chunks them as few times as possible - and stores what it fetched for as long as its {@code Cache-Control}
 * allows. The result carries the most restrictive directives of the cache hits' remaining lifetime and the fetch.
 *
 * <p>Resources are cached per type, id, requested includes, and whatever of the request is propagated and may change
 * the response: the sparse fieldset of the type and the custom query parameters.
 */
@Slf4j
public class CachingBatchFetcher<R extends DomainSettings> implements BatchFetcher<R> {

    private final BatchFetcher<R> delegate;
    private final CompoundDocsResourceCache cache;
    private final JsonApiResponseParser responseParser;
    private final List<Propagation> propagation;

    /**
     * @param responseParser parses resources served from the cache, which keeps them as JSON
     * @param propagation    what of the request is propagated - and so part of the cache key
     */
    public CachingBatchFetcher(BatchFetcher<R> delegate,
                               CompoundDocsResourceCache cache,
                               JsonApiResponseParser responseParser,
                               List<Propagation> propagation) {
        this.delegate = Validate.notNull(delegate, "delegate must not be null");
        this.cache = Validate.notNull(cache, "cache must not be null");
        this.responseParser = Validate.notNull(responseParser, "responseParser must not be null");
        this.propagation = List.copyOf(Validate.notNull(propagation, "propagation must not be null"));
    }

    @Override
    public FetchResult fetch(BatchFetch<R> batch, CompoundDocsRequest originalRequest) {
        if (batch.ids() == null || batch.ids().isEmpty()) {
            return FetchResult.empty();
        }
        ResourceType type = new ResourceType(batch.resourceType());
        Set<String> fields = fieldsOf(batch.resourceType(), originalRequest);
        Map<String, List<String>> queryParams = customQueryParamsOf(originalRequest);

        Set<CacheKey> keys = batch.ids().stream()
                .map(id -> new CacheKey(new IdAndType(id, type), batch.includes(), fields, queryParams))
                .collect(Collectors.toSet());
        Map<CacheKey, CacheResult> cacheHits = cache.getAll(keys);
        List<ParsedResource> resources = new ArrayList<>(cacheHits.values().stream()
                .map(cacheResult -> responseParser.parseResource(cacheResult.getResourceJson()))
                .toList());
        Set<String> missIds = keys.stream()
                .filter(key -> !cacheHits.containsKey(key))
                .map(CacheKey::getResourceId)
                .collect(Collectors.toSet());

        log.debug(
                "Cache lookup for type '{}': {} hits, {} misses", batch.resourceType(), cacheHits.size(), missIds.size()
        );
        if (missIds.isEmpty()) {
            return new FetchResult(resources, remainingLifetimeOf(cacheHits).getResult());
        }

        FetchResult fetched = delegate.fetch(batch.withIds(missIds), originalRequest);
        for (ParsedResource resource : fetched.resources()) {
            resources.add(resource);
            if (resource.idAndType() != null) {
                CacheKey key = new CacheKey(
                        new IdAndType(resource.idAndType().getId(), type),
                        batch.includes(),
                        fields,
                        queryParams
                );
                cache.put(key, resource.json(), fetched.directives());
            }
        }

        CacheControlAggregator aggregator = remainingLifetimeOf(cacheHits);
        aggregator.add(fetched.directives());
        return new FetchResult(resources, aggregator.getResult(), fetched.incompleteReason());
    }

    /**
     * @return an aggregator holding the shortest remaining lifetime of the cache hits, if there are any
     */
    private static CacheControlAggregator remainingLifetimeOf(Map<CacheKey, CacheResult> cacheHits) {
        CacheControlAggregator aggregator = new CacheControlAggregator();
        cacheHits.values().stream()
                .mapToLong(CacheResult::getRemainingTtlSeconds)
                .min()
                .ifPresent(minTtl -> {
                    if (minTtl > 0) {
                        aggregator.add(CacheControlDirectives.ofMaxAge(minTtl));
                    }
                });
        return aggregator;
    }

    /**
     * @return the propagated custom query parameters - a response may vary by them - or none if they aren't propagated
     */
    private Map<String, List<String>> customQueryParamsOf(CompoundDocsRequest originalRequest) {
        if (!propagation.contains(Propagation.CUSTOM_QUERY_PARAMS)) {
            return Collections.emptyMap();
        }
        return originalRequest.getCustomQueryParams();
    }

    /**
     * @return the propagated sparse fieldset of {@code resourceType}, or none if fields aren't propagated
     */
    private Set<String> fieldsOf(String resourceType, CompoundDocsRequest originalRequest) {
        if (!propagation.contains(Propagation.FIELDS)) {
            return Collections.emptySet();
        }
        Map<String, List<String>> fieldSets = originalRequest.getFieldSets();
        if (fieldSets == null || !fieldSets.containsKey(resourceType)) {
            return Collections.emptySet();
        }
        return new HashSet<>(fieldSets.get(resourceType));
    }

}
