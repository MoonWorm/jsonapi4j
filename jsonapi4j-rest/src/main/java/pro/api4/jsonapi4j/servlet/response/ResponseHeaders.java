package pro.api4.jsonapi4j.servlet.response;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;
import pro.api4.jsonapi4j.http.HttpHeaders;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

import static java.lang.ThreadLocal.withInitial;

/**
 * Propagates HTTP response headers from downstream service responses to the final client-facing response.
 *
 * <p>Uses a {@link ThreadLocal} store so that headers collected during request processing (potentially across
 * multiple downstream calls) are scoped to the current servlet thread. The thread-level scope matches
 * the request execution scope, giving each concurrent request its own independent header context.</p>
 *
 * <p>Typical lifecycle within a single request:</p>
 * <ol>
 *     <li>{@link #propagateCacheControl(HttpServletResponse)}, {@link #propagateCacheControl(CacheControlDirectives)},
 *         or {@link #propagateHeader(String, String)} — called during request processing to collect headers
 *         for propagation</li>
 *     <li>{@link #flush(HttpServletResponse)} — called by the response filter to apply all collected
 *         headers to the outgoing response and clean up the {@link ThreadLocal} store</li>
 * </ol>
 *
 * @see CacheControlParser
 * @see CacheControlDirectives
 */
public class ResponseHeaders {

    private static final int HARD_LIMIT = 100;

    private static final ThreadLocal<ConcurrentHashMap<String, List<String>>> HEADERS = withInitial(ConcurrentHashMap::new);

    /**
     * Extracts the {@code Cache-Control} header from a downstream service response and stores it
     * for later propagation to the client-facing response.
     *
     * <p>If the downstream response contains a non-blank {@code Cache-Control} header, its value is
     * parsed via {@link CacheControlParser#parse(String)} and stored in the {@link ThreadLocal} context.</p>
     *
     * @param downstreamServiceResponse the downstream HTTP response to extract {@code Cache-Control} from;
     *                                  {@code null} values are safely ignored
     */
    public static void propagateCacheControl(HttpServletResponse downstreamServiceResponse) {
        if (downstreamServiceResponse != null) {
            String cacheControlHeaderValue = downstreamServiceResponse.getHeader(HttpHeaders.CACHE_CONTROL.getName());
            if (StringUtils.isNotBlank(cacheControlHeaderValue)) {
                ResponseHeaders.propagateCacheControl(CacheControlParser.parse(cacheControlHeaderValue));
            }
        }
    }

    /**
     * Stores the given {@code Cache-Control} directives for later propagation to the client-facing response.
     *
     * <p>The directives are formatted back into a header string via {@link CacheControlParser#format(CacheControlDirectives)}
     * and stored in the {@link ThreadLocal} context, replacing any previously stored {@code Cache-Control} value.</p>
     *
     * @param cacheControlDirectives the parsed {@code Cache-Control} directives to propagate
     */
    public static void propagateCacheControl(CacheControlDirectives cacheControlDirectives) {
        HEADERS.get().put(
                HttpHeaders.CACHE_CONTROL.getName(),
                Collections.singletonList(CacheControlParser.format(cacheControlDirectives))
        );
    }

    /**
     * Stores an arbitrary header for later propagation to the client-facing response.
     *
     * <p>Multiple values for the same header name are accumulated and will each be added
     * as separate header entries when {@link #flush(HttpServletResponse)} is called.</p>
     *
     * @param header the header name
     * @param value  the header value
     */
    public static void propagateHeader(String header, String value) {
        if (HEADERS.get().size() > HARD_LIMIT) {
            throw new IllegalStateException("Max headers limit reached: " + HARD_LIMIT);
        }
        HEADERS.get().computeIfAbsent(
                header,
                k -> new ArrayList<>()
        ).add(value);
    }

    /**
     * Applies all collected headers to the outgoing HTTP response and clears the {@link ThreadLocal} store.
     *
     * <p>{@code Cache-Control} receives special treatment:</p>
     * <ul>
     *     <li>Only propagated for {@code 2xx} status codes</li>
     *     <li>Only set if the response does not already carry a {@code Cache-Control} header
     *         (i.e. does not override an explicitly set value)</li>
     * </ul>
     *
     * <p>All other collected headers are added unconditionally. Multi-valued headers are added
     * as separate header entries.</p>
     *
     * <p>This method <strong>must</strong> be called at the end of request processing to prevent
     * {@link ThreadLocal} leaks (e.g. from a servlet filter's {@code finally} block).</p>
     *
     * @param response the outgoing HTTP response to write headers to
     */
    public static void flush(HttpServletResponse response) {
        // propagate Cache-Control only for 2xx status codes
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            String originalCacheControlHeaderValue = response.getHeader(HttpHeaders.CACHE_CONTROL.getName());
            List<String> propagatedControlHeaderValue = HEADERS.get().get(HttpHeaders.CACHE_CONTROL.getName());
            if (StringUtils.isBlank(originalCacheControlHeaderValue)
                    && propagatedControlHeaderValue != null
                    && propagatedControlHeaderValue.size() == 1) {
                response.addHeader(HttpHeaders.CACHE_CONTROL.getName(), propagatedControlHeaderValue.getFirst());
            }
        }

        HEADERS.get().remove(HttpHeaders.CACHE_CONTROL.getName());

        // propagate other headers
        HEADERS.get().forEach((header, value) -> {
            if (CollectionUtils.isNotEmpty(value)) {
                if (value.size() == 1) {
                    response.addHeader(header, value.getFirst());
                } else {
                    value.forEach(v -> {
                        if (v != null) {
                            response.addHeader(header, v);
                        }
                    });
                }
            }
        });

        HEADERS.remove();

    }

}
