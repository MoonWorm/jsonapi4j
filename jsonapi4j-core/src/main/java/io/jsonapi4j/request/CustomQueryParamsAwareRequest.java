package io.jsonapi4j.request;

import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;

public interface CustomQueryParamsAwareRequest {

    /**
     * Relevant for all JSON:API operations. For example,
     * <code>GET /countries?filter[id]=NO,FI,SE&myParam=123,321</code> request should return
     * <code>myParam</code> - <code>123</code>,<code>321</code> for this method.
     * <p>
     * Carries all non-JSON:API, unknown, or custom query params. All
     * specification-specific params can be retrieved by using the corresponding methods e.g.
     * {@link FiltersAwareRequest#getFilters()}, {@link SortAwareRequest#getSortBy()}, etc.
     *
     * @return the map where keys are custom param names and values - list of comma-separated values,
     * if no custom params specifies returns an empty map
     */
    Map<String, List<String>> getCustomQueryParams();

    default Map<String, String> asSingleValueMap() {
        return getCustomQueryParams()
                .entrySet()
                .stream()
                .collect(
                        toMap(
                                Map.Entry::getKey,
                                e -> e.getValue()
                                        .stream()
                                        .sorted()
                                        .collect(Collectors.joining(",")))
                );
    }

    default List<String> asListOfStrings() {
        return getCustomQueryParams()
                .entrySet()
                .stream()
                .map(e -> e.getKey() + "=" + String.join(",", e.getValue()))
                .sorted()
                .toList();
    }

    default String asString() {
        return String.join("&", asListOfStrings());
    }

    default String getQueryParamSingleValue(String queryParam) {
        if (getCustomQueryParams().containsKey(queryParam)) {
            List<String> values = getCustomQueryParams().get(queryParam);
            if (values != null && !values.isEmpty()) {
                return values.stream().filter(StringUtils::isNotBlank).findFirst().orElse(null);
            }
        }
        return null;
    }

}
