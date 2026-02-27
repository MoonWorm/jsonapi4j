package pro.api4.jsonapi4j.filter.cd;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.client.JsonApiHttpClient;
import pro.api4.jsonapi4j.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.stream.Collectors.toMap;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

public class CompoundDocsFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompoundDocsFilter.class);

    private static final Set<String> DISALLOWED_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "upgrade"
    );
    private static final Pattern RELATIONSHIP_OPERATION_URL_PATTERN = Pattern.compile("/[^/]+/[^/]+/relationships/([^/]+)");

    private CompoundDocsResolver resolver;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        ObjectMapper objectMapper = (ObjectMapper) filterConfig.getServletContext().getAttribute(OBJECT_MAPPER_ATT_NAME);
        Validate.notNull(objectMapper);

        JsonApi4jProperties properties = (JsonApi4jProperties) filterConfig.getServletContext().getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);
        Validate.notNull(properties);

        CompoundDocsProperties compoundDocsProperties = properties.getCompoundDocs();
        Validate.notNull(compoundDocsProperties);

        ExecutorService executorService = (ExecutorService) filterConfig.getServletContext().getAttribute(EXECUTOR_SERVICE_ATT_NAME);
        Validate.notNull(executorService);

        resolver = new CompoundDocsResolver(
                new CompoundDocsResolverConfig(
                        objectMapper,
                        new CompoundDocsResolverConfig.DefaultDomainUrlResolver(
                                MapUtils.emptyIfNull(compoundDocsProperties.getMapping())
                                        .entrySet()
                                        .stream()
                                        .collect(toMap(
                                                Map.Entry::getKey,
                                                e -> URI.create(e.getValue())
                                        ))),
                        executorService,
                        compoundDocsProperties.getMaxHops(),
                        compoundDocsProperties.getErrorStrategy()
                )
        );
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        if (isGetRequest(httpRequest.getMethod()) && isCompoundDocsProcessingIsNotDisabled(httpRequest)) {
            Set<String> includes = getIncludesQueryParam(httpRequest);
            if (includes != null && !includes.isEmpty()) {
                try (BufferedResponseWrapper responseWrapper = new BufferedResponseWrapper(httpResponse)) {
                    chain.doFilter(request, responseWrapper);

                    String responseBody = responseWrapper.getCaptureAsString();
                    if (is2xxResponseCode(responseWrapper.getStatus())) {
                        String responseBodyWithCompoundDocs;
                        Optional<String> relationshipNameOpt = getRelationshipNameFromRequestUri(httpRequest.getRequestURI());
                        if (relationshipNameOpt.isPresent()) {
                            responseBodyWithCompoundDocs = resolver.resolveCompoundDocsForRelationshipResponse(
                                    responseBody,
                                    getIncludesQueryParam(httpRequest),
                                    getOriginalRequestHeaders(httpRequest),
                                    relationshipNameOpt.get()
                            );
                        } else {
                            responseBodyWithCompoundDocs = resolver.resolveCompoundDocsForPrimaryResourceResponse(
                                    responseBody,
                                    getIncludesQueryParam(httpRequest),
                                    getOriginalRequestHeaders(httpRequest)
                            );
                        }

                        writeUtf8ResponseBody(response, responseBodyWithCompoundDocs);
                    } else {
                        writeUtf8ResponseBody(response, responseBody);
                    }
                } catch (Exception e) {
                    LOGGER.error("Failed to close streams for BufferedResponseWrapper", e);
                }
            } else {
                chain.doFilter(request, response);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean isGetRequest(String method) {
        return "GET".equals(method);
    }

    private Optional<String> getRelationshipNameFromRequestUri(String url) {
        Matcher matcher = RELATIONSHIP_OPERATION_URL_PATTERN.matcher(url);
        if (matcher.find()) {
            try {
                String relationshipName = matcher.group(1);
                return Optional.of(relationshipName);
            } catch (Exception e) {
                // do nothing
            }
        }
        return Optional.empty();
    }

    private boolean is2xxResponseCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

    private boolean isCompoundDocsProcessingIsNotDisabled(HttpServletRequest httpRequest) {
        return !Boolean.parseBoolean(httpRequest.getHeader(JsonApiHttpClient.X_DISABLE_COMPOUND_DOCS));
    }

    private Set<String> getIncludesQueryParam(HttpServletRequest httpRequest) {
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
            if (!DISALLOWED_HEADERS.contains(headerName.toLowerCase())) {
                originalRequestHeaders.put(headerName, httpRequest.getHeader(headerName));
            }
        }
        return MapUtils.unmodifiableMap(originalRequestHeaders);
    }

    private void writeUtf8ResponseBody(ServletResponse response, String responseBody) throws IOException {
        response.getOutputStream().write(responseBody.getBytes(StandardCharsets.UTF_8));
    }

}
