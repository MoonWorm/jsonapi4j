package pro.api4.jsonapi4j.compound.docs.cache;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Thread-safe, in-memory implementation of {@link CompoundDocsResourceCache}.
 *
 * <p>Stores cached resources in a {@link ConcurrentHashMap} with TTL-based expiration
 * derived from {@link CacheControlDirectives#getEffectiveTtlSeconds()}.
 * When the cache exceeds {@code maxSize}, expired entries are purged first,
 * then the least recently used (LRU) non-expired entry is evicted.
 *
 * <p>The {@code maxSize} is a soft cap — concurrent writes may briefly exceed it
 * by a small number of entries. This trade-off avoids lock contention on reads.
 *
 * @see CompoundDocsResourceCache
 * @see AbstractCompoundDocsResourceCache
 */
public class InMemoryCompoundDocsResourceCache extends AbstractCompoundDocsResourceCache {

    private final ConcurrentHashMap<CacheKey, CacheEntry> store;
    private final int maxSize;
    private final Clock clock;

    /**
     * Creates an in-memory cache with the given max size and clock.
     *
     * @param maxSize soft cap on number of cached entries, must be &gt; 0
     * @param clock   clock for computing expiration timestamps and remaining TTL
     */
    public InMemoryCompoundDocsResourceCache(int maxSize, Clock clock) {
        if (maxSize <= 0) {
            throw new IllegalArgumentException("maxSize must be > 0, got: " + maxSize);
        }
        this.maxSize = maxSize;
        this.clock = Validate.notNull(clock, "clock must not be null");
        this.store = new ConcurrentHashMap<>();
    }

    /**
     * Creates an in-memory cache with the given max size and system UTC clock.
     *
     * @param maxSize soft cap on number of cached entries, must be &gt; 0
     */
    public InMemoryCompoundDocsResourceCache(int maxSize) {
        this(maxSize, Clock.systemUTC());
    }

    /**
     * Retrieves a cached resource by key.
     *
     * <p>If the entry exists but is expired, it is lazily removed and empty is returned.
     * On a cache hit, the entry's last-access time is updated (for LRU eviction)
     * and the remaining TTL is computed dynamically from the clock.
     *
     * @param key the cache key
     * @return the cached result with remaining TTL, or empty if not cached or expired
     */
    @Override
    public Optional<CacheResult> get(CacheKey key) {
        CacheEntry entry = store.get(key);
        if (entry == null) {
            return Optional.empty();
        }

        Instant now = clock.instant();
        if (entry.isExpired(now)) {
            store.remove(key, entry);
            return Optional.empty();
        }

        entry.touch(now);
        return Optional.of(new CacheResult(entry.resourceJson, entry.remainingTtlSeconds(now)));
    }

    /**
     * Stores a resource in the cache. Only called when directives are cacheable
     * (enforced by {@link AbstractCompoundDocsResourceCache#put}).
     *
     * <p>Computes expiration from {@link CacheControlDirectives#getEffectiveTtlSeconds()}.
     * If the cache is at or above {@code maxSize}, triggers eviction:
     * first removes all expired entries, then evicts the least recently used
     * non-expired entry if still over the limit.
     *
     * @param key          the cache key
     * @param resourceJson the raw JSON string of the resource object
     * @param directives   the Cache-Control directives (guaranteed cacheable)
     */
    @Override
    protected void doPut(CacheKey key, String resourceJson, CacheControlDirectives directives) {
        Instant now = clock.instant();
        Instant expiresAt = now.plusSeconds(directives.getEffectiveTtlSeconds());
        CacheEntry entry = new CacheEntry(resourceJson, expiresAt, now);
        store.put(key, entry);

        if (store.size() > maxSize) {
            evict(now);
        }
    }

    /**
     * Two-phase eviction:
     * <ol>
     *   <li>Remove all expired entries</li>
     *   <li>If still over maxSize, remove the least recently used (oldest lastAccessedAt) non-expired entry</li>
     * </ol>
     */
    private void evict(Instant now) {
        // Phase 1 — remove all expired entries
        store.entrySet().removeIf(e -> e.getValue().isExpired(now));

        // Phase 2 — LRU eviction if still over limit
        if (store.size() > maxSize) {
            CacheKey lruKey = null;
            Instant oldestAccess = null;

            for (Map.Entry<CacheKey, CacheEntry> e : store.entrySet()) {
                Instant lastAccessed = e.getValue().lastAccessedAt;
                if (oldestAccess == null || lastAccessed.isBefore(oldestAccess)) {
                    oldestAccess = lastAccessed;
                    lruKey = e.getKey();
                }
            }

            if (lruKey != null) {
                store.remove(lruKey);
            }
        }
    }

    /**
     * Internal cache entry holding the resource JSON, expiration time,
     * and last access time for LRU eviction.
     */
    private static class CacheEntry {

        private final String resourceJson;
        private final Instant expiresAt;
        private volatile Instant lastAccessedAt;

        CacheEntry(String resourceJson,
                   Instant expiresAt,
                   Instant insertionTime) {
            this.resourceJson = resourceJson;
            this.expiresAt = expiresAt;
            this.lastAccessedAt = insertionTime;
        }

        boolean isExpired(Instant now) {
            return now.isAfter(expiresAt) || now.equals(expiresAt);
        }

        long remainingTtlSeconds(Instant now) {
            return Math.max(0, Duration.between(now, expiresAt).getSeconds());
        }

        void touch(Instant now) {
            this.lastAccessedAt = now;
        }
    }

}
