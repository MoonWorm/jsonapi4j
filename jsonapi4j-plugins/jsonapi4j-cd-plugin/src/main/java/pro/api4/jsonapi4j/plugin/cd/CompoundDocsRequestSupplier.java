package pro.api4.jsonapi4j.plugin.cd;

import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil;
import pro.api4.jsonapi4j.util.BaseUrls;

import java.text.MessageFormat;
import java.util.*;

import static java.util.stream.Collectors.toMap;

public class CompoundDocsRequestSupplier {

    private final String rootPath;

    public CompoundDocsRequestSupplier(String rootPath) {
        this.rootPath = rootPath;
    }

    public CompoundDocsRequest toCompoundDocsRequest(HttpServletRequest servletRequest) {
        Map<String, List<String>> allParams = getParams(servletRequest);

        return new CompoundDocsRequest(
                servletRequest.getMethod(),
                getIncludesQueryParam(servletRequest),
                JsonApiRequestParsingUtil.parseFieldSets(allParams),
                getOriginalRequestHeaders(servletRequest),
                servletRequest.getRequestURI(),
                JsonApiRequestParsingUtil.parseCustomQueryParams(allParams),
                resolveSelfBaseUrl(servletRequest)
        );
    }

    /**
     * Reconstructs the app's own JSON:API root from the incoming request — the exact authority the request arrived on
     * ({@code scheme://host:port}, as addressed by the client) plus the servlet context path and the configured
     * JSON:API root path. This lets unmapped, same-app resource types (notably the meta types) resolve against the live
     * endpoint without any configured base URL.
     */
    private String resolveSelfBaseUrl(HttpServletRequest servletRequest) {
        String baseUrl = MessageFormat.format(
                "{0}://{1}:{2}",
                servletRequest.getScheme(),
                servletRequest.getServerName(),
                String.valueOf(servletRequest.getServerPort())
        );
        return BaseUrls.join(baseUrl + servletRequest.getContextPath(), rootPath);
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

    private Map<String, List<String>> getParams(HttpServletRequest request) {
        return request.getParameterMap()
                .entrySet()
                .stream()
                .collect(toMap(Map.Entry::getKey, e -> Arrays.asList(e.getValue())));
    }

}
