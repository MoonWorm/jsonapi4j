package pro.api4.jsonapi4j.request;

import java.util.Map;

/**
 * Request mixin that provides access to HTTP request headers.
 */
public interface HeadersAwareRequest {

    Map<String, String> getHeaders();

    default String getHeader(String headerName) {
        return getHeaders().get(headerName);
    }

}
