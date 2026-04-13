package pro.api4.jsonapi4j.compound.docs.cache;

import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;

/**
 * Base class for {@link CompoundDocsResourceCache} implementations.
 *
 * <p>Enforces the {@link CacheControlDirectives#isCacheable()} check before storing.
 * Subclasses implement {@link #doPut(CacheKey, String, CacheControlDirectives)}
 * for the actual storage logic — it is only called when directives are cacheable.
 */
public abstract class AbstractCompoundDocsResourceCache implements CompoundDocsResourceCache {

    /**
     * Checks whether the directives allow caching, and if so, delegates to
     * {@link #doPut(CacheKey, String, CacheControlDirectives)}.
     * If {@code !directives.isCacheable()}, the call is silently skipped.
     *
     * <p>This method is {@code final} — subclasses must not override it.
     * Implement {@link #doPut} instead.
     */
    @Override
    public final void put(CacheKey key, String resourceJson, CacheControlDirectives directives) {
        if (!directives.isCacheable()) {
            return;
        }
        doPut(key, resourceJson, directives);
    }

    /**
     * Stores a resource in the cache. Only called when
     * {@code directives.isCacheable()} is {@code true}.
     *
     * @param key          the cache key
     * @param resourceJson the raw JSON string of the resource object
     * @param directives   the Cache-Control directives (guaranteed cacheable)
     */
    protected abstract void doPut(CacheKey key, String resourceJson, CacheControlDirectives directives);

}
