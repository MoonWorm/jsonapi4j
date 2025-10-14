package pro.api4.jsonapi4j.servlet.response.cache;

import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;

/**
 * Helps to proxy Cache-Control header value if there is something in the HTTP Response Headers of the downstream
 * services response.
 * ThreadLocal variable must be set explicitly after the corresponding downstream service invocation.
 * Thread-level scope matches request execution scope. Thus, we have different cache contexts per every parallel
 * request execution.
 */
public class CacheControlPropagator {

    private static final String CACHE_CONTROL_HEADER = "Cache-Control";
    private static final ThreadLocal<CacheControl> CACHE_CONTROL = new ThreadLocal<>();

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
            String cacheControlHeaderValue = downstreamServiceResponse.getHeader(CACHE_CONTROL_HEADER);
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
        CACHE_CONTROL.set(new CacheControl(cacheSettings));
    }

    /**
     * Propagates preserved Cache-Control value if response doesn't have its own Cache-Control
     * being set.
     *
     * @param response target response to where Cache-Control header value must be propagated
     */
    public static void propagateCacheControlIfNeeded(HttpServletResponse response) {
        if (response.getStatus() >= 200 && response.getStatus() < 300) {
            String cacheControlHeaderValue = response.getHeader(CACHE_CONTROL_HEADER);
            if (StringUtils.isBlank(cacheControlHeaderValue) && CACHE_CONTROL.get() != null) {
                response.addHeader(CACHE_CONTROL_HEADER, CACHE_CONTROL.get().getValue());
            }
            CACHE_CONTROL.remove();
        }

    }

    public static class CacheControl {

        private final String value;

        public CacheControl(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

    }

}
