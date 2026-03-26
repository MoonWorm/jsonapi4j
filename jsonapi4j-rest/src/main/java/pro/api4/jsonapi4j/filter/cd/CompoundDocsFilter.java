package pro.api4.jsonapi4j.filter.cd;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.DefaultCompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DefaultDomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.DomainUrlResolver;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.util.JsonApiRequestParsingUtil;

import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static java.util.stream.Collectors.toMap;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_PROPERTIES_ATT_NAME;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initExecutorService;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initObjectMapper;

@Slf4j
public class CompoundDocsFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompoundDocsFilter.class);

    private CompoundDocsResolver resolver;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Initializing {} ...", CompoundDocsFilter.class.getSimpleName());

        // TODO: extract CompoundDocsProperties same as OasProperties
        JsonApi4jProperties properties = (JsonApi4jProperties) filterConfig.getServletContext().getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);
        log.info("Applied {} from Servlet Context under {} attribute", JsonApi4jProperties.class.getSimpleName(), JSONAPI4J_PROPERTIES_ATT_NAME);

        CompoundDocsResolverConfig config = new CompoundDocsResolverConfig(
                properties.compoundDocs().enabled(),
                properties.compoundDocs().maxHops(),
                properties.compoundDocs().errorStrategy()
        );

        log.info("Effective compound docs settings: {}", config);

        if (config.isEnabled()) {
            DomainUrlResolver domainUrlResolver = new DefaultDomainUrlResolver(
                    MapUtils.emptyIfNull(properties.compoundDocs().mapping())
                            .entrySet()
                            .stream()
                            .collect(toMap(
                                    Map.Entry::getKey,
                                    e -> URI.create(e.getValue())
                            ))
            );

            ObjectMapper objectMapper = initObjectMapper(filterConfig.getServletContext());
            Validate.notNull(objectMapper);

            ExecutorService executorService = initExecutorService(filterConfig.getServletContext());
            Validate.notNull(executorService);

            resolver = new CompoundDocsResolver(
                    config,
                    domainUrlResolver,
                    objectMapper,
                    executorService
            );

            log.info("{} has been successfully composed", CompoundDocsResolver.class.getSimpleName());
            log.info("{} has been initialized", CompoundDocsFilter.class.getSimpleName());
        } else {
            log.info("{} has been initialized, but the feature is disabled", CompoundDocsFilter.class.getSimpleName());
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        CompoundDocsRequest compoundDocsRequest = new DefaultCompoundDocsRequest(
                httpRequest.getMethod(),
                getIncludesQueryParam(httpRequest),
                getSparseFieldsetsParams(httpRequest),
                getOriginalRequestHeaders(httpRequest),
                httpRequest.getRequestURI()
        );

        if (resolver != null && compoundDocsRequest.isProcessable()) {
            try (BufferedResponseWrapper responseWrapper = new BufferedResponseWrapper(httpResponse)) {
                chain.doFilter(request, responseWrapper);

                String responseBody = responseWrapper.getCaptureAsString();
                if (is2xxResponseCode(responseWrapper.getStatus())) {
                    String responseBodyWithCompoundDocs = resolver.resolveCompoundDocs(responseBody, compoundDocsRequest);
                    response.getWriter().write(responseBodyWithCompoundDocs);
                } else {
                    response.getWriter().write(responseBody);
                }
            } catch (Exception e) {
                LOGGER.error("Compound Document resolution process failed.", e);
                throw new RuntimeException(e);
            }
        } else {
            chain.doFilter(request, response);
        }
    }

    private boolean is2xxResponseCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
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
