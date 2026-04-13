package pro.api4.jsonapi4j.http.cache;

/**
 * Collects {@link CacheControlDirectives} from multiple downstream fetches and
 * computes the most restrictive aggregated result.
 *
 * <p>Restrictive directives ({@code no-store}, {@code no-cache}, {@code private})
 * are sticky — once any source sets them, the aggregated result includes them.
 * TTL values ({@code max-age}, {@code s-maxage}) use the minimum across all sources.
 *
 * <p>Instantiated once per compound docs resolution. Not thread-safe — directives
 * are added sequentially between hops (after {@code CompletableFuture.join()}).
 *
 * @see CacheControlParser#format(CacheControlDirectives)
 */
public class CacheControlAggregator {

    private Long minMaxAge;
    private Long minSMaxAge;
    private boolean noStore;
    private boolean noCache;
    private boolean privateCacheControl;
    private boolean hasDirectives;

    /**
     * Adds a set of Cache-Control directives to the aggregation.
     *
     * <p>{@code null} directives are ignored (no-op).
     *
     * @param directives the directives to add, or {@code null} to skip
     */
    public void add(CacheControlDirectives directives) {
        if (directives == null) {
            return;
        }
        hasDirectives = true;

        if (directives.isNoStore()) {
            noStore = true;
        }
        if (directives.isNoCache()) {
            noCache = true;
        }
        if (directives.isPrivateCacheControl()) {
            privateCacheControl = true;
        }
        if (directives.getMaxAge() != null) {
            minMaxAge = minMaxAge == null ? directives.getMaxAge()
                    : Math.min(minMaxAge, directives.getMaxAge());
        }
        if (directives.getSMaxAge() != null) {
            minSMaxAge = minSMaxAge == null ? directives.getSMaxAge()
                    : Math.min(minSMaxAge, directives.getSMaxAge());
        }
    }

    /**
     * Returns the aggregated Cache-Control directives, or {@code null} if no
     * directives were added.
     *
     * @return aggregated directives, or {@code null} if {@link #add} was never called
     *         with a non-null value
     */
    public CacheControlDirectives getResult() {
        if (!hasDirectives) {
            return null;
        }
        return new CacheControlDirectives(minMaxAge, minSMaxAge, noStore, noCache, privateCacheControl);
    }
}
