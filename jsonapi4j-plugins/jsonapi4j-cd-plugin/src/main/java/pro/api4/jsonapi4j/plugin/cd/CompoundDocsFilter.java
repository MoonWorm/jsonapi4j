package pro.api4.jsonapi4j.plugin.cd;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver;
import pro.api4.jsonapi4j.compound.docs.DomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;

import java.io.IOException;
import java.util.concurrent.ExecutorService;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initExecutorService;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.initObjectMapper;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_PLUGIN_DOMAIN_URL_RESOLVER_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.cd.init.JsonApi4jCompoundDocsServletContainerInitializer.COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME;

@Slf4j
public class CompoundDocsFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompoundDocsFilter.class);

    private final CompoundDocsRequestSupplier requestSupplier = new CompoundDocsRequestSupplier();
    private CompoundDocsResolver resolver;

    @Override
    public void init(FilterConfig filterConfig) {
        log.info("Initializing {} ...", CompoundDocsFilter.class.getSimpleName());

        CompoundDocsProperties cdProperties = (CompoundDocsProperties) filterConfig.getServletContext()
                .getAttribute(COMPOUND_DOCS_PLUGIN_PROPERTIES_ATT_NAME);
        Validate.notNull(cdProperties, "Compound Docs Properties are not found in ServletContext");

        DomainUrlResolver domainUrlResolver = (DomainUrlResolver) filterConfig.getServletContext()
                .getAttribute(COMPOUND_DOCS_PLUGIN_DOMAIN_URL_RESOLVER_ATT_NAME);
        Validate.notNull(domainUrlResolver, "Domain Url Resolver is not found in ServletContext");

        CompoundDocsResolverConfig config = new CompoundDocsResolverConfig(
                cdProperties.enabled(),
                cdProperties.maxHops(),
                cdProperties.maxIncludedResources(),
                cdProperties.errorStrategy(),
                cdProperties.propagation(),
                cdProperties.deduplicateResources(),
                cdProperties.httpConnectTimeoutMs(),
                cdProperties.httpTotalTimeoutMs()
        );

        log.info("Effective compound docs settings: {}", config);

        if (config.isEnabled()) {
            ObjectMapper objectMapper = initObjectMapper(filterConfig.getServletContext());
            ExecutorService executorService = initExecutorService(filterConfig.getServletContext());
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
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpServletResponse = (HttpServletResponse) servletResponse;

        CompoundDocsRequest compoundDocsRequest = requestSupplier.toCompoundDocsRequest(httpServletRequest);

        if (resolver != null && compoundDocsRequest.isProcessable()) {
            try (BufferedResponseWrapper responseWrapper = new BufferedResponseWrapper(httpServletResponse)) {
                chain.doFilter(servletRequest, responseWrapper);

                String responseBody = responseWrapper.getCaptureAsString();
                if (is2xxResponseCode(responseWrapper.getStatus())) {
                    String responseBodyWithCompoundDocs = resolver.resolveCompoundDocs(responseBody, compoundDocsRequest);
                    servletResponse.getWriter().write(responseBodyWithCompoundDocs);
                } else {
                    servletResponse.getWriter().write(responseBody);
                }
            } catch (Exception e) {
                LOGGER.error("Compound Document resolution process failed.", e);
                throw new RuntimeException(e);
            }
        } else {
            chain.doFilter(servletRequest, servletResponse);
        }
    }

    private boolean is2xxResponseCode(int statusCode) {
        return statusCode >= 200 && statusCode < 300;
    }


}
