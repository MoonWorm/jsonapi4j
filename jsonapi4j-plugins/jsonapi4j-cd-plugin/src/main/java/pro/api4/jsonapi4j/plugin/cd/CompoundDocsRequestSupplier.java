package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.http.HttpServletRequest;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil;

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class CompoundDocsRequestSupplier {

    public CompoundDocsRequest toCompoundDocsRequest(HttpServletRequest servletRequest) {
        return toCompoundDocsRequest(servletRequest, Set.of());
    }

    /**
     * @param rejectedIncludes include paths the app already rejected, so the request was served without them
     */
    public CompoundDocsRequest toCompoundDocsRequest(HttpServletRequest servletRequest, Set<String> rejectedIncludes) {
        Map<String, List<String>> allParams = getParams(servletRequest);

        return new CompoundDocsRequest(
                servletRequest.getMethod(),
                getIncludesQueryParam(servletRequest),
                rejectedIncludes.stream().sorted().toList(),
                JsonApiRequestParsingUtil.parseFieldSets(allParams),
                getOriginalRequestHeaders(servletRequest),
                servletRequest.getRequestURI(),
                JsonApiRequestParsingUtil.parseCustomQueryParams(allParams)
        );
    }

    private List<String> getIncludesQueryParam(HttpServletRequest httpRequest) {
        String[] value = httpRequest.getParameterValues(IncludeAwareRequest.INCLUDE_PARAM);
        if (value == null) {
            return null;
        }
        return JsonApiRequestParsingUtil.parseOriginalIncludes(Arrays.asList(value));
    }

    private Map<String, List<String>> getOriginalRequestHeaders(HttpServletRequest httpRequest) {
        Map<String, List<String>> originalRequestHeaders = new HashMap<>();
        for (Iterator<String> it = httpRequest.getHeaderNames().asIterator(); it.hasNext(); ) {
            String headerName = it.next();
            originalRequestHeaders.put(headerName, Collections.list(httpRequest.getHeaders(headerName)));
        }
        return originalRequestHeaders;
    }

    private Map<String, List<String>> getParams(HttpServletRequest request) {
        return request.getParameterMap()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue())));
    }

}
