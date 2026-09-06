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
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;
import pro.api4.jsonapi4j.http.cache.CacheControlParser;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_PLUGIN_DOMAIN_SETTINGS_RESOLVER_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME;

@Slf4j
public class CompoundDocsFilter implements Filter {

    private CompoundDocsRequestSupplier requestSupplier;
    private CompoundDocsResolver resolver;

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("Initializing {} ...", CompoundDocsFilter.class.getSimpleName());

        CompoundDocsProperties cdProperties = (CompoundDocsProperties) filterConfig.getServletContext()
                .getAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME);

        if (cdProperties != null && cdProperties.enabled()) {
            JsonApi4jProperties jsonApi4jProperties = (JsonApi4jProperties) filterConfig.getServletContext().getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);

            this.requestSupplier = new CompoundDocsRequestSupplier(jsonApi4jProperties.rootPath());

            DomainSettingsResolver domainSettingsResolver = (DomainSettingsResolver) filterConfig.getServletContext()
                    .getAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_SETTINGS_RESOLVER_ATT_NAME);

            CompoundDocsResolverConfig config = new CompoundDocsResolverConfig(
                    cdProperties.enabled(),
                    cdProperties.maxHops(),
                    cdProperties.maxIncludedResources(),
                    cdProperties.errorStrategy(),
                    cdProperties.propagation(),
                    cdProperties.deduplicateResources(),
                    cdProperties.httpConnectTimeoutMs(),
                    cdProperties.httpTotalTimeoutMs(),
                    cdProperties.cache() != null ? cdProperties.cache().enabled() : Boolean.parseBoolean(CompoundDocsProperties.Cache.DEFAULT_CACHE_ENABLED),
                    cdProperties.cache() != null ? cdProperties.cache().maxSize() : Integer.parseInt(CompoundDocsProperties.Cache.DEFAULT_CACHE_MAX_SIZE)
            );
            log.debug("Effective {} settings: {}", CompoundDocsResolverConfig.class.getSimpleName(), config);

            ObjectMapper objectMapper = initObjectMapper(filterConfig.getServletContext());
            ExecutorService executorService = initExecutorService(filterConfig.getServletContext());

            CompoundDocsResourceCache cache = (CompoundDocsResourceCache) filterConfig
                    .getServletContext()
                    .getAttribute(COMPOUND_DOCS_PLUGIN_CACHE_ATT_NAME);

            resolver = new CompoundDocsResolver(
                    config,
                    domainSettingsResolver,
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

        if (this.resolver == null || this.requestSupplier == null) {
            log.debug("{} has not been initialized, CD plugin initialization failer or plugin is disabled", CompoundDocsFilter.class.getSimpleName());
            chain.doFilter(servletRequest, servletResponse);
        } else {
            HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
            HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

            CompoundDocsRequest compoundDocsRequest = requestSupplier.toCompoundDocsRequest(httpServletRequest);

            if (compoundDocsRequest.isProcessable()) {
                try (BufferedResponseWrapper responseWrapper = new BufferedResponseWrapper(httpServletResponse)) {
                    chain.doFilter(servletRequest, responseWrapper);

                    String responseBody = responseWrapper.getCaptureAsString();
                    if (is2xxResponseCode(responseWrapper.getStatus())) {
                        CompoundDocsResult result = resolver.resolveCompoundDocs(responseBody, compoundDocsRequest);
                        applyCacheControlHeader(httpServletResponse, responseWrapper, result);
                        servletResponse.getWriter().write(result.responseBody());
                    } else {
                        servletResponse.getWriter().write(responseBody);
                    }
                } catch (Exception e) {
                    log.error("Compound Document resolution process failed.", e);
                    throw new RuntimeException(e);
                }
            } else {
                chain.doFilter(servletRequest, servletResponse);
            }
        }
    }

    private void applyCacheControlHeader(HttpServletResponse response,
                                         BufferedResponseWrapper responseWrapper,
                                         CompoundDocsResult result) {
        CacheControlAggregator aggregator = new CacheControlAggregator();

        String primaryCacheControl = responseWrapper.getHeader("Cache-Control");
        if (primaryCacheControl != null) {
            aggregator.add(CacheControlParser.parse(primaryCacheControl));
        }

        aggregator.add(result.cacheControlDirectives());

        CacheControlDirectives aggregated = aggregator.getResult();
        if (aggregated != null) {
            String headerValue = CacheControlParser.format(aggregated);
            if (headerValue != null) {
                response.setHeader("Cache-Control", headerValue);
            }
        }
    }

    private boolean is2xxResponseCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }

}
