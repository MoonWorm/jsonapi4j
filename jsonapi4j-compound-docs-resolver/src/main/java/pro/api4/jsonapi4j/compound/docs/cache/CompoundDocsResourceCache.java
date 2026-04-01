package pro.api4.jsonapi4j.compound.docs.cache;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * SPI for caching individual JSON:API resources during compound document resolution.
 *
 * <p>Resources are cached by composite key ({@link CacheKey}) which includes
 * resource type, ID, downstream includes, and sparse fieldsets.
 * Cache implementations must be thread-safe.
 *
 * <p>Implementations should extend {@link AbstractCompoundDocsResourceCache}
 * which enforces the {@link CacheControlDirectives#isCacheable()} check before storing.
 */
public interface CompoundDocsResourceCache {

    /**
     * Retrieves a cached resource by key.
     *
     * @param key the cache key
     * @return the cached result with remaining TTL, or empty if not cached or expired
     */
    Optional<CacheResult> get(CacheKey key);

    /**
     * Stores a resource in the cache.
     *
     * <p>Implementations extending {@link AbstractCompoundDocsResourceCache}
     * inherit automatic {@link CacheControlDirectives#isCacheable()} checking.
     * Direct implementations of this interface must handle non-cacheable
     * directives themselves.
     *
     * @param key          the cache key
     * @param resourceJson the raw JSON string of the resource object
     * @param directives   the Cache-Control directives from the downstream response;
     *                     implementation uses these to determine TTL
     */
    void put(CacheKey key, String resourceJson, CacheControlDirectives directives);

    /**
     * Retrieves multiple cached resources by key.
     *
     * <p>Default implementation delegates to {@link #get(CacheKey)} for each key.
     * Implementations backed by network caches (e.g., Redis) should override
     * for batch efficiency.
     *
     * @param keys the cache keys to look up
     * @return map of keys to cache results for cache hits only; misses are omitted
     */
    default Map<CacheKey, CacheResult> getAll(Collection<CacheKey> keys) {
        Map<CacheKey, CacheResult> results = new HashMap<>();
        for (CacheKey key : keys) {
            get(key).ifPresent(result -> results.put(key, result));
        }
        return results;
    }

    /**
     * Stores multiple resources in the cache, all sharing the same Cache-Control directives.
     *
     * <p>Default implementation delegates to {@link #put(CacheKey, String, CacheControlDirectives)}
     * for each entry. Implementations backed by network caches should override
     * for batch efficiency.
     *
     * @param resources  map of cache keys to raw JSON strings
     * @param directives the Cache-Control directives from the downstream response,
     *                   shared by all resources in this batch (one HTTP response = one Cache-Control header)
     */
    default void putAll(Map<CacheKey, String> resources, CacheControlDirectives directives) {
        resources.forEach((key, json) -> put(key, json, directives));
    }

}
