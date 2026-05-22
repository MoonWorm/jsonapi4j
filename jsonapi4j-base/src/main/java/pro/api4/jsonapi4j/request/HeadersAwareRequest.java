package pro.api4.jsonapi4j.request;

import java.util.Map;

public interface HeadersAwareRequest {

    Map<String, String> getHeaders();

    default String getHeader(String headerName) {
        return getHeaders().get(headerName);
    }

}
