package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil;

import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The request with some {@code include} paths removed - those the app rejected - so it can be served again with the
 * rest of them. Without any path left, the {@code include} parameter is gone altogether.
 */
class IncludesRemovedRequest extends HttpServletRequestWrapper {

    private final Map<String, String[]> parameters;

    IncludesRemovedRequest(HttpServletRequest request, Set<String> removedIncludes) {
        super(request);
        this.parameters = Collections.unmodifiableMap(withoutIncludes(request.getParameterMap(), removedIncludes));
    }

    private static Map<String, String[]> withoutIncludes(Map<String, String[]> parameters, Set<String> removedIncludes) {
        Map<String, String[]> result = new LinkedHashMap<>(parameters);
        String[] includeValues = parameters.get(IncludeAwareRequest.INCLUDE_PARAM);
        if (includeValues == null) {
            return result;
        }
        List<String> keptIncludes = JsonApiRequestParsingUtil.parseOriginalIncludes(Arrays.asList(includeValues))
                .stream()
                .filter(include -> !removedIncludes.contains(include))
                .toList();
        if (keptIncludes.isEmpty()) {
            result.remove(IncludeAwareRequest.INCLUDE_PARAM);
        } else {
            result.put(IncludeAwareRequest.INCLUDE_PARAM, new String[]{String.join(",", keptIncludes)});
        }
        return result;
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return parameters;
    }

    @Override
    public String[] getParameterValues(String name) {
        return parameters.get(name);
    }

    @Override
    public String getParameter(String name) {
        String[] values = parameters.get(name);
        return values == null || values.length == 0 ? null : values[0];
    }

    @Override
    public Enumeration<String> getParameterNames() {
        return Collections.enumeration(parameters.keySet());
    }

}
