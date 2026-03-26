package pro.api4.jsonapi4j.filter.cd;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver;
import pro.api4.jsonapi4j.compound.docs.DefaultDomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.DomainUrlResolver;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import static java.util.stream.Collectors.toMap;
import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.*;

@Slf4j
public class CompoundDocsFilter implements Filter {

    private static final Logger LOGGER = LoggerFactory.getLogger(CompoundDocsFilter.class);

    private final CompoundDocsRequestSupplier requestSupplier = new CompoundDocsRequestSupplier();
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
