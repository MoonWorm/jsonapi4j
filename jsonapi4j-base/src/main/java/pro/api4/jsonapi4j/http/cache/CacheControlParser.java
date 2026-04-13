package pro.api4.jsonapi4j.http.cache;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses {@code Cache-Control} HTTP header values into {@link CacheControlDirectives}.
 *
 * <p>Handles the following directives (case-insensitive):
 * {@code max-age}, {@code s-maxage}, {@code no-store}, {@code no-cache}, {@code private}.
 * Unknown directives are silently ignored.
 *
 * @see <a href="https://httpwg.org/specs/rfc9111.html#field.cache-control">RFC 9111 - Cache-Control</a>
 */
public final class CacheControlParser {

    private CacheControlParser() {
    }

    /**
     * Parses a Cache-Control header value into structured directives.
     *
     * @param headerValue the raw Cache-Control header value (e.g. "max-age=300, no-store"),
     *                    may be null or empty
     * @return parsed directives; returns {@link CacheControlDirectives#NON_CACHEABLE}
     *         for null, empty, or entirely unparseable input
     */
    public static CacheControlDirectives parse(String headerValue) {
        if (headerValue == null || headerValue.isBlank()) {
            return CacheControlDirectives.NON_CACHEABLE;
        }

        Long maxAge = null;
        Long sMaxAge = null;
        boolean noStore = false;
        boolean noCache = false;
        boolean privateCacheControl = false;

        String[] tokens = headerValue.split(",");
        for (String token : tokens) {
            String directive = token.trim().toLowerCase();

            if (directive.equals("no-store")) {
                noStore = true;
            } else if (directive.equals("no-cache")) {
                noCache = true;
            } else if (directive.equals("private")) {
                privateCacheControl = true;
            } else if (directive.startsWith("max-age=") && !directive.startsWith("s-maxage=")) {
                maxAge = parseNonNegativeLong(directive.substring("max-age=".length()));
            } else if (directive.startsWith("s-maxage=")) {
                sMaxAge = parseNonNegativeLong(directive.substring("s-maxage=".length()));
            }
        }

        return new CacheControlDirectives(maxAge, sMaxAge, noStore, noCache, privateCacheControl);
    }

    /**
     * Formats {@link CacheControlDirectives} into a {@code Cache-Control} header value string.
     *
     * @param directives the directives to format, must not be {@code null}
     * @return the formatted header value, or {@code null} if the directives produce
     *         no output (e.g. {@link CacheControlDirectives#NON_CACHEABLE})
     */
    public static String format(CacheControlDirectives directives) {
        List<String> parts = new ArrayList<>();
        if (directives.isNoStore()) {
            parts.add("no-store");
        }
        if (directives.isNoCache()) {
            parts.add("no-cache");
        }
        if (directives.isPrivateCacheControl()) {
            parts.add("private");
        }
        if (directives.getSMaxAge() != null) {
            parts.add("s-maxage=" + directives.getSMaxAge());
        }
        if (directives.getMaxAge() != null) {
            parts.add("max-age=" + directives.getMaxAge());
        }
        return parts.isEmpty() ? null : String.join(", ", parts);
    }

    private static Long parseNonNegativeLong(String value) {
        try {
            long parsed = Long.parseLong(value.trim());
            return parsed >= 0 ? parsed : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
