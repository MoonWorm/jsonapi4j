package pro.api4.jsonapi4j.http.cache;

import lombok.*;

/**
 * Immutable value object representing parsed Cache-Control HTTP header directives.
 * Used to drive caching decisions for compound document resource fetching.
 *
 * <p>Instances are created via {@link CacheControlParser#parse(String)} or the
 * {@link #NON_CACHEABLE} constant.
 *
 * @see <a href="https://httpwg.org/specs/rfc9111.html">RFC 9111 - HTTP Caching</a>
 */
@Getter
@AllArgsConstructor(access = AccessLevel.PACKAGE)
@EqualsAndHashCode
@ToString
public class CacheControlDirectives {

    /**
     * Represents a non-cacheable response (no Cache-Control header, or unparseable).
     * {@link #isCacheable()} returns {@code false}.
     */
    public static final CacheControlDirectives NON_CACHEABLE =
            new CacheControlDirectives(null, null, false, false, false);

    /**
     * Creates directives with only a {@code max-age} value set.
     * Useful for synthesizing directives from cache hit remaining TTLs.
     *
     * @param maxAgeSeconds the max-age value in seconds
     * @return directives with only max-age set
     */
    public static CacheControlDirectives ofMaxAge(long maxAgeSeconds) {
        return new CacheControlDirectives(maxAgeSeconds, null, false, false, false);
    }

    private final Long maxAge;
    private final Long sMaxAge;
    private final boolean noStore;
    private final boolean noCache;
    private final boolean privateCacheControl;

    /**
     * Returns the effective TTL in seconds.
     * {@code s-maxage} takes precedence over {@code max-age} per HTTP spec (RFC 9111).
     *
     * @return effective TTL in seconds, or {@code null} if neither directive is present
     */
    public Long getEffectiveTtlSeconds() {
        return sMaxAge != null ? sMaxAge : maxAge;
    }

    /**
     * Returns {@code true} if the response is cacheable:
     * <ul>
     *   <li>{@code no-store}, {@code no-cache}, and {@code private} are all absent</li>
     *   <li>AND effective TTL is non-null and greater than zero</li>
     * </ul>
     */
    public boolean isCacheable() {
        Long effectiveTtl = getEffectiveTtlSeconds();
        return !noStore && !noCache && !privateCacheControl
                && effectiveTtl != null && effectiveTtl > 0;
    }

}
