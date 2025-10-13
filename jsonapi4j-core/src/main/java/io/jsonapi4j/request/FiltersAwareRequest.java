package io.jsonapi4j.request;

import org.apache.commons.collections4.CollectionUtils;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface FiltersAwareRequest {

    Pattern PATTERN = Pattern.compile("^filter\\[(.*)]$");

    static String getFilterParam(String filterName) {
        return String.format("filter[%s]", filterName);
    }

    static String getFilterParamWithValue(String filterName, List<String> values) {
        if (filterName == null || filterName.isEmpty() || CollectionUtils.isEmpty(values)) {
            return null;
        }
        return String.format("%s=%s", getFilterParam(filterName), String.join(",", values));
    }

    static boolean isJsonApiFilterParam(String paramName) {
        if (paramName == null) {
            return false;
        }
        String regex = "^filter\\[.*]$";
        return paramName.matches(regex);
    }

    static String extractFilterName(String paramName) {
        if (paramName == null) {
            return null;
        }
        Matcher matcher = PATTERN.matcher(paramName);
        if (matcher.matches()) {
            return matcher.group(1);
        }
        return null;
    }

    /**
     * Relevant for all multi resource operations, e.g.<code>GET /countries?filter[id]=NO,FI,SE&filter[region]=europe,africa</code>.
     * For the example above this method will return a map for two keys: <code>id</code> and <code>region</code>. With
     * <code>NO,FI,SE</code> and <code>europe,africa</code> correspondingly.
     * <p>
     * Refer JSON:API specification for more details: <a href="https://jsonapi.org/format/#fetching-filtering">filtering</a>
     * <p>
     * For all multi resource operations it's highly recommended to implement at least a filter-by-id because
     * the framework is relying on this implementation for its own needs where possible. Other filters and
     * the multi-filter support is optional and is up to the developer implementation and domain specifics.
     *
     * @return the map where keys are the filter dimensions e.g. 'id', 'region' and values are list of values of the
     * corresponding dimensions
     */
    Map<String, List<String>> getFilters();

}
