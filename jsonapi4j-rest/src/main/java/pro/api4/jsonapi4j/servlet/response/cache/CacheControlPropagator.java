package pro.api4.jsonapi4j.servlet.response.cache;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;
import pro.api4.jsonapi4j.http.HttpHeaders;

/**
 * Helps to proxy Cache-Control header value if there is something in the HTTP Response Headers of the downstream
 * services response.
 * ThreadLocal variable must be set explicitly after the corresponding downstream service invocation.
 * Thread-level scope matches request execution scope. Thus, we have different cache contexts per every parallel
 * request execution.
 */
public class CacheControlPropagator {

    private static final ThreadLocal<CacheControlDirectives> CACHE_CONTROL = new ThreadLocal<>();

    /**
     * Trying to set Cache-Control header value from the downstream service response.
     * This value will be later propagated for the main server response.
     * This logic relies on of {@link #CACHE_CONTROL} Thread-local variable.
     * Each HTTP request is processed is a separate thread under the servlet container. Thus, {@link #CACHE_CONTROL}
     * value will be different for each processing HTTP requests.
     *
     * @param downstreamServiceResponse Response Entity of the underlying service that contains HTTP headers
     */
    public static void propagateCacheControl(HttpServletResponse downstreamServiceResponse) {
        if (downstreamServiceResponse != null) {
            String cacheControlHeaderValue = downstreamServiceResponse.getHeader(HttpHeaders.CACHE_CONTROL.getName());
            if (StringUtils.isNotBlank(cacheControlHeaderValue)) {
                CacheControlPropagator.propagateCacheControl(cacheControlHeaderValue);
            }
        }
    }

    /**
     * Explicitly sets cache-control header value that should be propagated.
     * This logic relies on of {@link #CACHE_CONTROL} Thread-local variable.
     * Each HTTP request is processed is a separate thread under the servlet container. Thus, {@link #CACHE_CONTROL}
     * value will be different for each processing HTTP requests.
     *
     * @param cacheSettings Cache-Control http header valid value
     */
    public static void propagateCacheControl(String cacheSettings) {
        CACHE_CONTROL.set(CacheControlParser.parse(cacheSettings));
    }

    /**
     * Propagates preserved Cache-Control value if response doesn't have its own Cache-Control
     * being set.
     *
     * @param response target response to where Cache-Control header value must be propagated
     */
    public static void propagateCacheControlIfNeeded(HttpServletResponse response) {
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            String cacheControlHeaderValue = response.getHeader(HttpHeaders.CACHE_CONTROL.getName());
            if (StringUtils.isBlank(cacheControlHeaderValue) && CACHE_CONTROL.get() != null) {
                response.addHeader(HttpHeaders.CACHE_CONTROL.getName(), CacheControlParser.format(CACHE_CONTROL.get()));
            }
            CACHE_CONTROL.remove();
        }

    }

}
