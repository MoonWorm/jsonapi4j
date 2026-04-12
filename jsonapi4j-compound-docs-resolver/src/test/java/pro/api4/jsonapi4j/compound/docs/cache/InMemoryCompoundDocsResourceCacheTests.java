package pro.api4.jsonapi4j.compound.docs.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InMemoryCompoundDocsResourceCacheTests {

    private static final String RESOURCE_JSON = "{\"type\":\"countries\",\"id\":\"FI\"}";
    private static final CacheControlDirectives CACHEABLE_300S = CacheControlParser.parse("max-age=300");
    private static final CacheControlDirectives CACHEABLE_60S = CacheControlParser.parse("max-age=60");

    // --- Constructor ---

    @Test
    void constructor_zeroMaxSize_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new InMemoryCompoundDocsResourceCache(0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_negativeMaxSize_throwsIllegalArgumentException() {
        assertThatThrownBy(() -> new InMemoryCompoundDocsResourceCache(-1))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_nullClock_throwsNullPointerException() {
        assertThatThrownBy(() -> new InMemoryCompoundDocsResourceCache(100, null))
                .isInstanceOf(NullPointerException.class);
    }

    @Test
    void constructor_validMaxSize_createsCache() {
        var cache = new InMemoryCompoundDocsResourceCache(100);

        assertThat(cache).isNotNull();
    }

    // --- Basic put/get ---

    @Test
    void get_afterPut_returnsCachedResult() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CACHEABLE_300S);

        assertThat(cache.get(key)).isPresent();
        assertThat(cache.get(key).get().getResourceJson()).isEqualTo(RESOURCE_JSON);
        assertThat(cache.get(key).get().getRemainingTtlSeconds()).isEqualTo(300);
    }

    @Test
    void get_missingKey_returnsEmpty() {
        var cache = new InMemoryCompoundDocsResourceCache(100);

        assertThat(cache.get(CacheKey.of("countries", "FI"))).isEmpty();
    }

    @Test
    void put_sameKeyTwice_overwritesPrevious() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, "{\"version\":1}", CACHEABLE_300S);
        cache.put(key, "{\"version\":2}", CACHEABLE_300S);

        assertThat(cache.get(key).get().getResourceJson()).isEqualTo("{\"version\":2}");
    }

    @Test
    void put_differentKeys_storedIndependently() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_300S);
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_300S);

        assertThat(cache.get(key1).get().getResourceJson()).isEqualTo("{\"id\":\"FI\"}");
        assertThat(cache.get(key2).get().getResourceJson()).isEqualTo("{\"id\":\"NO\"}");
    }

    // --- TTL expiration ---

    @Test
    void get_beforeExpiration_returnsResult() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CACHEABLE_60S);

        clock.advance(Duration.ofSeconds(59));
        assertThat(cache.get(key)).isPresent();
    }

    @Test
    void get_exactlyAtExpiration_returnsEmpty() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CACHEABLE_60S);

        clock.advance(Duration.ofSeconds(60));
        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void get_afterExpiration_returnsEmpty() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CACHEABLE_60S);
        assertThat(cache.get(key)).isPresent();

        clock.advance(Duration.ofSeconds(61));
        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void get_afterExpiration_removesEntry() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(2, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_60S);
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_300S);

        clock.advance(Duration.ofSeconds(61));

        // Access expired entry — triggers lazy removal
        assertThat(cache.get(key1)).isEmpty();

        // Put a new entry — if key1 was not removed, this would trigger eviction of key2
        CacheKey key3 = CacheKey.of("countries", "SE");
        cache.put(key3, "{\"id\":\"SE\"}", CACHEABLE_300S);

        assertThat(cache.get(key2)).isPresent();
        assertThat(cache.get(key3)).isPresent();
    }

    @Test
    void get_remainingTtl_computedDynamically() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CACHEABLE_300S);
        assertThat(cache.get(key).get().getRemainingTtlSeconds()).isEqualTo(300);

        clock.advance(Duration.ofSeconds(100));
        assertThat(cache.get(key).get().getRemainingTtlSeconds()).isEqualTo(200);
    }

    // --- TTL source ---

    @Test
    void put_sMaxAgePresent_usesSMaxAgeForTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key = CacheKey.of("countries", "FI");
        CacheControlDirectives directives = CacheControlParser.parse("max-age=300, s-maxage=120");

        cache.put(key, RESOURCE_JSON, directives);

        assertThat(cache.get(key).get().getRemainingTtlSeconds()).isEqualTo(120);

        clock.advance(Duration.ofSeconds(121));
        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void put_onlyMaxAge_usesMaxAgeForTtl() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(100, clock);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CACHEABLE_300S);

        assertThat(cache.get(key).get().getRemainingTtlSeconds()).isEqualTo(300);
    }

    // --- Non-cacheable directives (inherited guard) ---

    @Test
    void put_noStoreDirective_doesNotStore() {
        var cache = new InMemoryCompoundDocsResourceCache(100);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CacheControlParser.parse("no-store"));

        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void put_noCacheDirective_doesNotStore() {
        var cache = new InMemoryCompoundDocsResourceCache(100);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CacheControlParser.parse("no-cache"));

        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void put_privateDirective_doesNotStore() {
        var cache = new InMemoryCompoundDocsResourceCache(100);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CacheControlParser.parse("private, max-age=300"));

        assertThat(cache.get(key)).isEmpty();
    }

    @Test
    void put_nonCacheableConstant_doesNotStore() {
        var cache = new InMemoryCompoundDocsResourceCache(100);
        CacheKey key = CacheKey.of("countries", "FI");

        cache.put(key, RESOURCE_JSON, CacheControlDirectives.NON_CACHEABLE);

        assertThat(cache.get(key)).isEmpty();
    }

    // --- Max-size eviction ---

    @Test
    void put_belowMaxSize_noEviction() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(10, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_300S);
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_300S);

        assertThat(cache.get(key1)).isPresent();
        assertThat(cache.get(key2)).isPresent();
    }

    @Test
    void put_exceedsMaxSize_evictsLruEntry() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(2, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");
        CacheKey key3 = CacheKey.of("countries", "SE");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_300S);
        clock.advance(Duration.ofSeconds(1));
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_300S);
        clock.advance(Duration.ofSeconds(1));

        // key1 was accessed least recently — should be evicted
        cache.put(key3, "{\"id\":\"SE\"}", CACHEABLE_300S);

        assertThat(cache.get(key1)).isEmpty();        // evicted (LRU)
        assertThat(cache.get(key2)).isPresent();       // kept
        assertThat(cache.get(key3)).isPresent();       // just inserted
    }

    @Test
    void put_exceedsMaxSize_recentlyAccessedEntryKept() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(2, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");
        CacheKey key3 = CacheKey.of("countries", "SE");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_300S);
        clock.advance(Duration.ofSeconds(1));
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_300S);
        clock.advance(Duration.ofSeconds(1));

        // Access key1 — makes it recently used
        cache.get(key1);
        clock.advance(Duration.ofSeconds(1));

        // key2 is now LRU — should be evicted
        cache.put(key3, "{\"id\":\"SE\"}", CACHEABLE_300S);

        assertThat(cache.get(key1)).isPresent();       // recently accessed, kept
        assertThat(cache.get(key2)).isEmpty();          // evicted (LRU)
        assertThat(cache.get(key3)).isPresent();        // just inserted
    }

    @Test
    void put_exceedsMaxSizeWithExpiredEntries_evictsExpiredFirst() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(2, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");
        CacheKey key3 = CacheKey.of("countries", "SE");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_60S);   // expires at T+60
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_300S);   // expires at T+300

        clock.advance(Duration.ofSeconds(61));  // key1 expired

        cache.put(key3, "{\"id\":\"SE\"}", CACHEABLE_300S);

        assertThat(cache.get(key1)).isEmpty();         // expired and evicted
        assertThat(cache.get(key2)).isPresent();        // not expired, kept
        assertThat(cache.get(key3)).isPresent();        // just inserted
    }

    @Test
    void put_exceedsMaxSizeAllExpired_clearsAllExpired() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(2, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");
        CacheKey key3 = CacheKey.of("countries", "SE");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_60S);
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_60S);

        clock.advance(Duration.ofSeconds(61));  // both expired

        cache.put(key3, "{\"id\":\"SE\"}", CACHEABLE_300S);

        assertThat(cache.get(key1)).isEmpty();
        assertThat(cache.get(key2)).isEmpty();
        assertThat(cache.get(key3)).isPresent();
    }

    @Test
    void put_maxSizeOne_onlyOneEntryAtATime() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(1, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_300S);
        assertThat(cache.get(key1)).isPresent();

        clock.advance(Duration.ofSeconds(1));
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_300S);

        assertThat(cache.get(key1)).isEmpty();
        assertThat(cache.get(key2)).isPresent();
    }

    // --- LRU behavior ---

    @Test
    void get_updatesLastAccessTime_affectsEvictionOrder() {
        MutableClock clock = new MutableClock(Instant.parse("2025-01-01T00:00:00Z"));
        var cache = new InMemoryCompoundDocsResourceCache(2, clock);
        CacheKey key1 = CacheKey.of("countries", "FI");
        CacheKey key2 = CacheKey.of("countries", "NO");
        CacheKey key3 = CacheKey.of("countries", "SE");

        cache.put(key1, "{\"id\":\"FI\"}", CACHEABLE_300S);
        clock.advance(Duration.ofSeconds(1));
        cache.put(key2, "{\"id\":\"NO\"}", CACHEABLE_300S);
        clock.advance(Duration.ofSeconds(1));

        // Access key1 — makes it recently used
        cache.get(key1);
        clock.advance(Duration.ofSeconds(1));

        // key2 is now LRU — should be evicted
        cache.put(key3, "{\"id\":\"SE\"}", CACHEABLE_300S);

        assertThat(cache.get(key1)).isPresent();       // recently accessed, kept
        assertThat(cache.get(key2)).isEmpty();          // evicted (LRU)
        assertThat(cache.get(key3)).isPresent();        // just inserted
    }

    // --- Thread safety ---

    @Test
    void concurrentPutAndGet_noCorruption() throws Exception {
        var cache = new InMemoryCompoundDocsResourceCache(1000);
        int threadCount = 10;
        int opsPerThread = 500;
        var latch = new CountDownLatch(threadCount);
        var errors = new AtomicInteger(0);

        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            new Thread(() -> {
                try {
                    for (int i = 0; i < opsPerThread; i++) {
                        CacheKey key = CacheKey.of("type", "id-" + threadId + "-" + i);
                        cache.put(key, "{\"id\":\"" + i + "\"}", CACHEABLE_300S);
                        cache.get(key); // may or may not be present (eviction)
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            }).start();
        }

        latch.await(10, TimeUnit.SECONDS);
        assertThat(errors.get()).isZero();
    }

}
