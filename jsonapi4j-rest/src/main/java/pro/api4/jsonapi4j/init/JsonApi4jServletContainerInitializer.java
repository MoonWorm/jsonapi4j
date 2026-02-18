package pro.api4.jsonapi4j.init;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.apache.commons.collections4.MapUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig.DefaultDomainUrlResolver;
import pro.api4.jsonapi4j.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.filter.cd.CompoundDocsFilter;
import pro.api4.jsonapi4j.filter.principal.PrincipalResolvingFilter;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;
import pro.api4.jsonapi4j.servlet.request.body.RequestBodyCachingFilter;

import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.util.stream.Collectors.toMap;
import static pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader.loadConfig;

public class JsonApi4jServletContainerInitializer implements ServletContainerInitializer {

    public static final String JSONAPI4J_DISPATCHER_SERVLET_NAME = "jsonApi4jDispatcherServlet";
    public static final String JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME = "jsonapi4jAccessControlFilter";
    public static final String JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME = "jsonapi4jRequestBodyCachingFilter";
    public static final String JSONAPI4J_COMPOUND_DOCS_FILTER_NAME = "jsonapi4jCompoundDocsFilter";

    public static final String JSONAPI4J_PROPERTIES_ATT_NAME = "jsonApi4jProperties";
    public static final String JSONAPI4J_ATT_NAME = "jsonApi4j";
    public static final String EXECUTOR_SERVICE_ATT_NAME = "jsonApi4jExecutorService";
    public static final String DOMAIN_REGISTRY_ATT_NAME = "jsonapi4jDomainRegistry";
    public static final String OPERATION_REGISTRY_ATT_NAME = "jsonapi4jOperationRegistry";
    public static final String PLUGINS_ATT_NAME = "jsonapi4jPlugins";
    public static final String ERROR_HANDLER_FACTORY_ATT_NAME = "jsonapi4jErrorHandlerFactory";
    public static final String OBJECT_MAPPER_ATT_NAME = "jsonApi4jObjectMapper";
    public static final String PRINCIPAL_RESOLVER_ATT_NAME = "jsonapi4jPrincipalResolver";

    private static final Logger LOG = LoggerFactory.getLogger(JsonApi4jServletContainerInitializer.class);

    @Override
    public void onStartup(Set<Class<?>> hooks, ServletContext servletContext) {
        JsonApi4jProperties properties = loadConfig(servletContext);
        initObjectMapper(servletContext);

        JsonApi4j jsonApi4j = (JsonApi4j) servletContext.getAttribute(JSONAPI4J_ATT_NAME);
        if (jsonApi4j == null) {
            LOG.warn("JsonApi4j not found in servlet context. Trying to compose an instance.");
            DomainRegistry domainRegistry = initDomainRegistry(servletContext);
            OperationsRegistry operationsRegistry = initOperationRegistry(servletContext);
            List<JsonApi4jPlugin> plugins = initPlugins(servletContext);
            ExecutorService executorService = initExecutorService(servletContext);
            jsonApi4j = JsonApi4j.builder()
                    .domainRegistry(domainRegistry)
                    .operationsRegistry(operationsRegistry)
                    .plugins(plugins)
                    .executor(executorService)
                    .build();
            servletContext.setAttribute(JSONAPI4J_ATT_NAME, jsonApi4j);
        }

        // ------------------
        // dispatcher servlet
        // ------------------
        String dispatcherServletMapping = properties.getRootPath() + "/*";
        registerDispatcherServlet(
                servletContext,
                dispatcherServletMapping
        );

        // -------
        // filters
        // -------

        registerPrincipalResolvingFilter(servletContext, dispatcherServletMapping);

        registerRequestBodyCachingFilter(servletContext, dispatcherServletMapping);
        if (properties.getCompoundDocs().isEnabled()) {
            initExecutorService(servletContext);
            registerCompoundDocsFilter(
                    servletContext,
                    dispatcherServletMapping
            );
        }
    }

    private void registerPrincipalResolvingFilter(ServletContext servletContext, String rootPath) {
        initJsonApi4jPrincipalResolver(servletContext);

        FilterRegistration.Dynamic filter = servletContext.addFilter(JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME, new PrincipalResolvingFilter());
        filter.addMappingForUrlPatterns(
                null, // DispatcherType.REQUEST is used by default
                false, // supposed to be matched before any declared filter mappings of the ServletContext
                rootPath
        );
    }

    private void registerRequestBodyCachingFilter(ServletContext servletContext, String rootPath) {
        FilterRegistration.Dynamic filter = servletContext.addFilter(JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME, new RequestBodyCachingFilter());
        filter.addMappingForUrlPatterns(
                null, // DispatcherType.REQUEST is used by default
                false, // supposed to be matched before any declared filter mappings of the ServletContext
                rootPath
        );
    }

    private void registerCompoundDocsFilter(ServletContext servletContext,
                                            String rootPath) {

        FilterRegistration.Dynamic filter = servletContext.addFilter(
                JSONAPI4J_COMPOUND_DOCS_FILTER_NAME,
                new CompoundDocsFilter()
        );
        filter.addMappingForUrlPatterns(
                null, // DispatcherType.REQUEST is used by default
                false, // supposed to be matched before any declared filter mappings of the ServletContext
                rootPath
        );
    }

    private void initJsonApi4jPrincipalResolver(ServletContext servletContext) {
        PrincipalResolver pr = (PrincipalResolver) servletContext.getAttribute(PRINCIPAL_RESOLVER_ATT_NAME);
        if (pr == null) {
            LOG.info("JsonApi4jPrincipalResolver not found in servlet context. Setting the default DefaultJsonApi4jPrincipalResolver.");
            pr = new DefaultPrincipalResolver();
            servletContext.setAttribute(PRINCIPAL_RESOLVER_ATT_NAME, pr);
        }
    }

    private void registerDispatcherServlet(ServletContext servletContext,
                                           String servletMapping) {
        ServletRegistration.Dynamic dispatcherServlet = servletContext.addServlet(
                JSONAPI4J_DISPATCHER_SERVLET_NAME,
                new JsonApi4jDispatcherServlet()
        );
        dispatcherServlet.addMapping(servletMapping);
    }

    private ExecutorService initExecutorService(ServletContext servletContext) {
        ExecutorService es = (ExecutorService) servletContext.getAttribute(EXECUTOR_SERVICE_ATT_NAME);
        if (es == null) {
            es = Executors.newCachedThreadPool();
            servletContext.setAttribute(EXECUTOR_SERVICE_ATT_NAME, es);
        }
        return es;
    }

    public static DomainRegistry initDomainRegistry(ServletContext servletContext) {
        DomainRegistry dr = (DomainRegistry) servletContext.getAttribute(DOMAIN_REGISTRY_ATT_NAME);
        if (dr == null) {
            LOG.warn("DomainRegistry not found in servlet context. Setting an empty DomainRegistry.");
            dr = DomainRegistry.empty();
            servletContext.setAttribute(DOMAIN_REGISTRY_ATT_NAME, dr);
        }
        return dr;
    }

    public static OperationsRegistry initOperationRegistry(ServletContext servletContext) {
        OperationsRegistry or = (OperationsRegistry) servletContext.getAttribute(OPERATION_REGISTRY_ATT_NAME);
        if (or == null) {
            LOG.warn("JsonApiOperationsRegistry not found in servlet context. Setting an empty JsonApiOperationsRegistry.");
            or = OperationsRegistry.empty();
            servletContext.setAttribute(OPERATION_REGISTRY_ATT_NAME, or);
        }
        return or;
    }

    public List<JsonApi4jPlugin> initPlugins(ServletContext servletContext) {
        //noinspection unchecked
        List<JsonApi4jPlugin> plugins = (List<JsonApi4jPlugin>) servletContext.getAttribute(PLUGINS_ATT_NAME);
        if (plugins == null) {
            LOG.warn("List<JsonApiPlugin> not found in servlet context. Setting an empty list.");
            plugins = Collections.emptyList();
            servletContext.setAttribute(PLUGINS_ATT_NAME, plugins);
        }
        return plugins;
    }

    private void initObjectMapper(ServletContext servletContext) {
        ObjectMapper om = (ObjectMapper) servletContext.getAttribute(OBJECT_MAPPER_ATT_NAME);
        if (om == null) {
            LOG.info("ObjectMapper not found in servlet context. Setting a default ObjectMapper.");
            om = new ObjectMapper();
            om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            om.registerModule(new JavaTimeModule());
            om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
            servletContext.setAttribute(OBJECT_MAPPER_ATT_NAME, om);
        }
    }

}
