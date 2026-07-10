package pro.api4.jsonapi4j.util;

import org.apache.commons.lang3.StringUtils;

/**
 * Small, null-safe helpers for composing HTTP URLs from a base and a path: trailing-slash normalization and
 * single-separator joining. Used when building the compound-docs self/downstream request URLs.
 */
public final class BaseUrls {

    private BaseUrls() {
    }

    /**
     * @param url a base URL, or {@code null}
     * @return {@code url} stripped of surrounding whitespace and a single trailing {@code /}; {@code null} when
     * {@code url} is {@code null}
     */
    public static String stripTrailingSlash(String url) {
        if (url == null) {
            return null;
        }
        String stripped = url.strip();
        return stripped.endsWith("/") ? stripped.substring(0, stripped.length() - 1) : stripped;
    }

    /**
     * Joins a base URL and a path with exactly one {@code /} separator. A blank {@code path} yields the base
     * (trailing slash stripped); a blank {@code base} yields the normalized path.
     *
     * @param base the base URL (e.g. {@code http://localhost:8080}), or {@code null}/blank
     * @param path the path to append (e.g. {@code /jsonapi} or {@code jsonapi}), or {@code null}/blank
     * @return the joined URL, normalized to a single {@code /} between base and path
     */
    public static String join(String base, String path) {
        String normalizedBase = stripTrailingSlash(base);
        if (StringUtils.isBlank(path)) {
            return normalizedBase;
        }
        String stripped = path.strip();
        String relative = stripped.startsWith("/") ? stripped : "/" + stripped;
        return normalizedBase == null ? relative : normalizedBase + relative;
    }

}
