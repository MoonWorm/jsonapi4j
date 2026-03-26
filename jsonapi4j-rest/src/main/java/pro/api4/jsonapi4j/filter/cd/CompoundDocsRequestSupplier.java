package pro.api4.jsonapi4j.filter.cd;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DefaultCompoundDocsRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil;

import java.util.*;

import static java.util.stream.Collectors.toMap;

public class CompoundDocsRequestSupplier {

    public CompoundDocsRequest toCompoundDocsRequest(HttpServletRequest servletRequest) {
        return new DefaultCompoundDocsRequest(
                servletRequest.getMethod(),
                getIncludesQueryParam(servletRequest),
                getSparseFieldsetsParams(servletRequest),
                getOriginalRequestHeaders(servletRequest),
                servletRequest.getRequestURI()
        );
    }

    private List<String> getIncludesQueryParam(HttpServletRequest httpRequest) {
        String[] value = httpRequest.getParameterValues(IncludeAwareRequest.INCLUDE_PARAM);
        if (value == null) {
            return null;
        }
        return JsonApiRequestParsingUtil.parseOriginalIncludes(Arrays.asList(value));
    }

    private Map<String, String> getOriginalRequestHeaders(HttpServletRequest httpRequest) {
        Map<String, String> originalRequestHeaders = new HashMap<>();
        for (Iterator<String> it = httpRequest.getHeaderNames().asIterator(); it.hasNext(); ) {
            String headerName = it.next();
            originalRequestHeaders.put(headerName, httpRequest.getHeader(headerName));
        }
        return MapUtils.unmodifiableMap(originalRequestHeaders);
    }

    private Map<String, List<String>> getSparseFieldsetsParams(HttpServletRequest httpRequest) {
        return JsonApiRequestParsingUtil.parseFieldSets(getParams(httpRequest));
    }

    private Map<String, List<String>> getParams(HttpServletRequest request) {
        return request.getParameterMap()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue())));
    }

}
