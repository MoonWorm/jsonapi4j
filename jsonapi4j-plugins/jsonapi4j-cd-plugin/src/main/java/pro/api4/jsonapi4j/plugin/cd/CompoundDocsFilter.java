package pro.api4.jsonapi4j.plugin.cd;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResult;
import pro.api4.jsonapi4j.compound.docs.DomainSettingsResolver;
import pro.api4.jsonapi4j.compound.docs.IncludesChecker;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorsDocResponseWriter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;

import static pro.api4.jsonapi4j.http.HttpHeaders.CACHE_CONTROL;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_PLUGIN_DOMAIN_SETTINGS_RESOLVER_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME;

@Slf4j
public class CompoundDocsFilter implements Filter {

    private CompoundDocsRequestSupplier requestSupplier;
    private SelfFallbackRouting routing;
    private ErrorsDocResponseWriter errorsDocResponseWriter;
    private IncludesChecker includesChecker;
    private CompoundDocsResolver resolver;

    public CompoundDocsFilter() {
    }

    CompoundDocsFilter(CompoundDocsRequestSupplier requestSupplier,
                       SelfFallbackRouting routing,
                       ErrorsDocResponseWriter errorsDocResponseWriter,
                       IncludesChecker includesChecker,
                       CompoundDocsResolver resolver) {
        this.requestSupplier = requestSupplier;
        this.routing = routing;
        this.errorsDocResponseWriter = errorsDocResponseWriter;
        this.includesChecker = includesChecker;
        this.resolver = resolver;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        log.info("Initializing {} ...", CompoundDocsFilter.class.getSimpleName());

        CompoundDocsProperties cdProperties = (CompoundDocsProperties) filterConfig.getServletContext()
                .getAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME);

        if (cdProperties != null && cdProperties.enabled()) {
            JsonApi4jProperties jsonApi4jProperties = (JsonApi4jProperties) filterConfig.getServletContext().getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);

            DomainSettingsResolver domainSettingsResolver = (DomainSettingsResolver) filterConfig.getServletContext()
                    .getAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_SETTINGS_RESOLVER_ATT_NAME);

            this.requestSupplier = new CompoundDocsRequestSupplier();
            this.routing = new SelfFallbackRouting(
                    domainSettingsResolver,
                    cdProperties,
                    jsonApi4jProperties.rootPath()
            );

            CompoundDocsResolverConfig config = new CompoundDocsResolverConfig(
                    cdProperties.enabled(),
                    cdProperties.maxHops(),
                    cdProperties.unsupportedIncludes(),
                    cdProperties.maxIncludedResources(),
                    cdProperties.errorStrategy(),
                    cdProperties.propagation(),
                    cdProperties.deduplication(),
                    cdProperties.httpConnectTimeoutMs(),
                    cdProperties.httpTotalTimeoutMs(),
                    cdProperties.cacheEnabled(),
                    cdProperties.cacheMaxSize()
            );
            log.debug("Effective {} settings: {}", CompoundDocsResolverConfig.class.getSimpleName(), config);
            this.includesChecker = IncludesChecker.from(config);

            ObjectMapper objectMapper = initObjectMapper(filterConfig.getServletContext());
            ExecutorService executorService = initExecutorService(filterConfig.getServletContext());
            this.errorsDocResponseWriter = new ErrorsDocResponseWriter(
                    initJsonApi4j(filterConfig.getServletContext()).getErrorHandlers(),
                    objectMapper
            );

            CompoundDocsResourceCache cache = (CompoundDocsResourceCache) filterConfig
                    .getServletContext()
                    .getAttribute(COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME);

            resolver = new CompoundDocsResolver(
                    config,
                    objectMapper,
                    executorService,
                    cache
            );
            log.debug("{} has been successfully composed", CompoundDocsResolver.class.getSimpleName());

            log.info("{} has been initialized", CompoundDocsFilter.class.getSimpleName());
        } else {
            log.info(
                    "{} has not been initialized, {} is disabled",
                    CompoundDocsFilter.class.getSimpleName(),
                    JsonApiCompoundDocsPlugin.class.getSimpleName()
            );
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {

        if (this.resolver == null || this.includesChecker == null || this.requestSupplier == null || this.routing == null) {
            log.debug("{} has not been initialized, CD plugin initialization failer or plugin is disabled", CompoundDocsFilter.class.getSimpleName());
            chain.doFilter(servletRequest, servletResponse);
        } else {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

            CompoundDocsRequest compoundDocsRequest = requestSupplier.toCompoundDocsRequest(httpServletRequest);

            if (compoundDocsRequest.isProcessable()) {
                try {
                    includesChecker.check(compoundDocsRequest);
                } catch (RuntimeException e) {
                    errorsDocResponseWriter.write(httpServletResponse, e);
                    return;
                }
                BufferedResponseWrapper responseWrapper = new BufferedResponseWrapper(httpServletResponse);
                chain.doFilter(servletRequest, responseWrapper);

                if (!is2xxResponseCode(responseWrapper.getStatus())) {
                    writeBody(httpServletResponse, responseWrapper.getCapturedBody());
                    return;
                }
                CompoundDocsResult result;
                try {
                    result = resolver.resolveCompoundDocs(
                            responseWrapper.getCapturedBodyAsString(),
                            compoundDocsRequest,
                            routing.forRequest(httpServletRequest)
                    );
                } catch (RuntimeException e) {
                    httpServletResponse.setHeader(
                            CACHE_CONTROL.getName(),
                            CacheControlParser.format(CacheControlDirectives.NO_STORE)
                    );
                    errorsDocResponseWriter.write(httpServletResponse, e);
                    return;
                }
                applyCacheControlHeader(httpServletResponse, responseWrapper, result);
                writeBody(httpServletResponse, result.responseBody().getBytes(StandardCharsets.UTF_8));
            } else {
                chain.doFilter(servletRequest, servletResponse);
            }
        }
    }

    private void applyCacheControlHeader(HttpServletResponse response,
                                         BufferedResponseWrapper responseWrapper,
                                         CompoundDocsResult result) {
        CacheControlAggregator aggregator = new CacheControlAggregator();

        String primaryCacheControl = responseWrapper.getHeader(CACHE_CONTROL.getName());
        if (primaryCacheControl != null) {
            aggregator.add(CacheControlParser.parse(primaryCacheControl));
        }

        aggregator.add(result.cacheControlDirectives());

        CacheControlDirectives aggregated = aggregator.getResult();
        if (aggregated != null) {
            String headerValue = CacheControlParser.format(aggregated);
            if (headerValue != null) {
                response.setHeader(CACHE_CONTROL.getName(), headerValue);
            }
        }
    }

    private void writeBody(HttpServletResponse response, byte[] body) throws IOException {
        if (body.length == 0) {
            return;
        }
        response.setContentLength(body.length);
        response.getOutputStream().write(body);
    }

    private boolean is2xxResponseCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

}
