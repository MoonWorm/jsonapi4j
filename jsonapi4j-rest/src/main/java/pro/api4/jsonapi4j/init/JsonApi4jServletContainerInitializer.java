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
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.Jsr380ErrorHandlers;

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
        ObjectMapper objectMapper = initObjectMapper(servletContext);
        ExecutorService executorService = initExecutorService(servletContext);
        DomainRegistry domainRegistry = initDomainRegistry(servletContext);
        OperationsRegistry operationsRegistry = initOperationRegistry(servletContext);
        List<JsonApi4jPlugin> plugins = initPlugins(servletContext);

        // dispatcher servlet
        String dispatcherServletMapping = properties.getRootPath() + "/*";
        registerDispatcherServlet(
                servletContext,
                dispatcherServletMapping,
                domainRegistry,
                operationsRegistry,
                plugins,
                objectMapper,
                executorService
        );

        // filters
        registerPrincipalResolvingFilter(servletContext, dispatcherServletMapping);
        registerRequestBodyCachingFilter(servletContext, dispatcherServletMapping);
        if (properties.getCompoundDocs().isEnabled()) {
            registerCompoundDocsFilter(
                    servletContext,
                    dispatcherServletMapping,
                    objectMapper,
                    executorService,
                    properties.getCompoundDocs()
            );
        }
    }

    private void registerPrincipalResolvingFilter(ServletContext servletContext, String rootPath) {
        PrincipalResolver principalResolver = initJsonApi4jPrincipalResolver(servletContext);
        FilterRegistration.Dynamic filter = servletContext.addFilter(JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME, new PrincipalResolvingFilter(principalResolver));
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
                                            String rootPath,
                                            ObjectMapper objectMapper,
                                            ExecutorService executorService,
                                            CompoundDocsProperties properties) {
        CompoundDocsResolver compoundDocsResolver = new CompoundDocsResolver(
                new CompoundDocsResolverConfig(
                        objectMapper,
                        new DefaultDomainUrlResolver(
                                MapUtils.emptyIfNull(properties.getMapping())
                                        .entrySet()
                                        .stream()
                                        .collect(toMap(
                                                Map.Entry::getKey,
                                                e -> URI.create(e.getValue())
                                        ))),
                        executorService,
                        properties.getMaxHops(),
                        properties.getErrorStrategy()
                )
        );
        FilterRegistration.Dynamic filter = servletContext.addFilter(
                JSONAPI4J_COMPOUND_DOCS_FILTER_NAME,
                new CompoundDocsFilter(compoundDocsResolver)
        );
        filter.addMappingForUrlPatterns(
                null, // DispatcherType.REQUEST is used by default
                false, // supposed to be matched before any declared filter mappings of the ServletContext
                rootPath
        );
    }

    private PrincipalResolver initJsonApi4jPrincipalResolver(ServletContext servletContext) {
        PrincipalResolver pr = (PrincipalResolver) servletContext.getAttribute(PRINCIPAL_RESOLVER_ATT_NAME);
        if (pr == null) {
            LOG.info("JsonApi4jPrincipalResolver not found in servlet context. Setting the default DefaultJsonApi4jPrincipalResolver.");
            pr = new DefaultPrincipalResolver();
        }
        return pr;
    }

    private void registerDispatcherServlet(ServletContext servletContext,
                                           String servletMapping,
                                           DomainRegistry domainRegistry,
                                           OperationsRegistry operationsRegistry,
                                           List<JsonApi4jPlugin> plugins,
                                           ObjectMapper objectMapper,
                                           ExecutorService executorService) {
        ErrorHandlerFactoriesRegistry errorHandlerFactory = initErrorHandlerFactory(servletContext);

        ServletRegistration.Dynamic dispatcherServlet = servletContext.addServlet(
                JSONAPI4J_DISPATCHER_SERVLET_NAME,
                new JsonApi4jDispatcherServlet(
                        domainRegistry,
                        operationsRegistry,
                        plugins,
                        executorService,
                        errorHandlerFactory,
                        objectMapper
                )
        );
        dispatcherServlet.addMapping(servletMapping);
    }

    private ExecutorService initExecutorService(ServletContext servletContext) {
        ExecutorService es = (ExecutorService) servletContext.getAttribute(EXECUTOR_SERVICE_ATT_NAME);
        if (es == null) {
            es = Executors.newCachedThreadPool();
        }
        return es;
    }

    public static DomainRegistry initDomainRegistry(ServletContext servletContext) {
        DomainRegistry dr = (DomainRegistry) servletContext.getAttribute(DOMAIN_REGISTRY_ATT_NAME);
        if (dr == null) {
            LOG.warn("DomainRegistry not found in servlet context. Setting an empty DomainRegistry.");
            dr = DomainRegistry.empty();
        }
        return dr;
    }

    public static OperationsRegistry initOperationRegistry(ServletContext servletContext) {
        OperationsRegistry or = (OperationsRegistry) servletContext.getAttribute(OPERATION_REGISTRY_ATT_NAME);
        if (or == null) {
            LOG.warn("JsonApiOperationsRegistry not found in servlet context. Setting an empty JsonApiOperationsRegistry.");
            or = OperationsRegistry.empty();
        }
        return or;
    }

    private List<JsonApi4jPlugin> initPlugins(ServletContext servletContext) {
        //noinspection unchecked
        List<JsonApi4jPlugin> plugins = (List<JsonApi4jPlugin>) servletContext.getAttribute(PLUGINS_ATT_NAME);
        if (plugins == null) {
            LOG.warn("List<JsonApiPlugin> not found in servlet context. Setting an empty list.");
            plugins = Collections.emptyList();
        }
        return plugins;
    }

    private ErrorHandlerFactoriesRegistry initErrorHandlerFactory(ServletContext servletContext) {
        ErrorHandlerFactoriesRegistry aehf = (ErrorHandlerFactoriesRegistry) servletContext.getAttribute(ERROR_HANDLER_FACTORY_ATT_NAME);
        if (aehf == null) {
            LOG.info("AggregatableErrorHandlerFactory not found in servlet context. Setting a default ErrorHandlerFactory.");
            aehf = new JsonApi4jErrorHandlerFactoriesRegistry();
            aehf.registerAll(new DefaultErrorHandlerFactory());
            aehf.registerAll(new Jsr380ErrorHandlers());
        }
        return aehf;
    }

    private ObjectMapper initObjectMapper(ServletContext servletContext) {
        ObjectMapper om = (ObjectMapper) servletContext.getAttribute(OBJECT_MAPPER_ATT_NAME);
        if (om == null) {
            LOG.info("ObjectMapper not found in servlet context. Setting a default ObjectMapper.");
            om = new ObjectMapper();
            om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            om.registerModule(new JavaTimeModule());
            om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return om;
    }

}
