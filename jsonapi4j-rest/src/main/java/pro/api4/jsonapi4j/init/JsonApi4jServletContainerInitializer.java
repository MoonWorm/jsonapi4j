package pro.api4.jsonapi4j.init;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.Jsr380ErrorHandlers;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolver;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig.ErrorStrategy;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;
import pro.api4.jsonapi4j.servlet.request.body.RequestBodyCachingFilter;
import pro.api4.jsonapi4j.servlet.filter.ac.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.servlet.filter.ac.JsonApi4jAccessControlFilter;
import pro.api4.jsonapi4j.servlet.filter.ac.PrincipalResolver;
import pro.api4.jsonapi4j.servlet.filter.cd.CompoundDocsFilter;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pro.api4.jsonapi4j.config.CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE;
import static pro.api4.jsonapi4j.config.CompoundDocsProperties.JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE;

public class JsonApi4jServletContainerInitializer implements ServletContainerInitializer {

    public static final String JSONAPI4J_DISPATCHER_SERVLET_NAME = "jsonApi4jDispatcherServlet";
    public static final String JSONAPI4J_ACCESS_CONTROL_FILTER_NAME = "jsonapi4jAccessControlFilter";
    public static final String JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME = "jsonapi4jRequestBodyCachingFilter";
    public static final String JSONAPI4J_COMPOUND_DOCS_FILTER_NAME = "jsonapi4jCompoundDocsFilter";

    public static final String JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_INIT_PARAM_NAME = "jsonapi4jCompoundDocsMaxHops";
    public static final String JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_INIT_PARAM_NAME = "jsonapi4jCompoundDocsErrorStrategy";

    public static final String JSONAPI4J_ROOT_PATH_ATT_NAME = "jsonapi4jRootPath";
    public static final String EXECUTOR_SERVICE_ATT_NAME = "jsonApi4jExecutorService";
    public static final String DOMAIN_REGISTRY_ATT_NAME = "jsonapi4jDomainRegistry";
    public static final String OPERATION_REGISTRY_ATT_NAME = "jsonapi4jOperationRegistry";
    public static final String ACCESS_CONTROL_EVALUATOR_ATT_NAME = "jsonapi4jAccessControlEvaluator";
    public static final String ERROR_HANDLER_FACTORY_ATT_NAME = "jsonapi4jErrorHandlerFactory";
    public static final String OBJECT_MAPPER_ATT_NAME = "jsonApi4jObjectMapper";
    public static final String PRINCIPAL_RESOLVER_ATT_NAME = "jsonapi4jPrincipalResolver";
    public static final String DOMAIN_URL_RESOLVER_ATT_NAME = "jsonapi4jDomainUrlResolver";

    private static final Logger LOG = LoggerFactory.getLogger(JsonApi4jServletContainerInitializer.class);

    @Override
    public void onStartup(Set<Class<?>> hooks, ServletContext servletContext) {
        String rootPath = getRootPath(servletContext);

        ObjectMapper objectMapper = initObjectMapper(servletContext);
        ExecutorService executorService = initExecutorService(servletContext);

        // dispatcher servlet
        registerDispatcherServlet(servletContext, rootPath, objectMapper, executorService);

        // filters
        registerAccessControlFilter(servletContext, rootPath);
        registerRequestBodyCachingFilter(servletContext, rootPath);
        registerCompoundDocsFilter(servletContext, rootPath, objectMapper, executorService);
    }

    private String getRootPath(ServletContext servletContext) {
        String rootPath = (String) servletContext.getAttribute(JSONAPI4J_ROOT_PATH_ATT_NAME);
        if (rootPath == null) {
            LOG.info("JsonApiRootPath not found in servlet context. Setting the default value: {}", JSONAPI4J_ROOT_PATH_ATT_NAME);
            rootPath = JsonApi4jProperties.JSONAPI4J_DEFAULT_ROOT_PATH + "/*";
        }
        return rootPath;
    }

    private void registerAccessControlFilter(ServletContext servletContext, String rootPath) {
        PrincipalResolver principalResolver = initJsonApi4jPrincipalResolver(servletContext);
        FilterRegistration.Dynamic filter = servletContext.addFilter(JSONAPI4J_ACCESS_CONTROL_FILTER_NAME, new JsonApi4jAccessControlFilter(principalResolver));
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
                                            ExecutorService executorService) {
        CompoundDocsResolverConfig.DomainUrlResolver domainUrlResolver =
                (CompoundDocsResolverConfig.DomainUrlResolver) servletContext.getAttribute(DOMAIN_URL_RESOLVER_ATT_NAME);
        int compoundDocsMaxHops = getCompoundDocsMaxHops(servletContext);
        ErrorStrategy errorStrategy = getCompoundDocsErrorStrategy(servletContext);
        CompoundDocsResolver compoundDocsResolver = new CompoundDocsResolver(
                new CompoundDocsResolverConfig(
                        objectMapper,
                        domainUrlResolver,
                        executorService,
                        compoundDocsMaxHops,
                        errorStrategy
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

    private int getCompoundDocsMaxHops(ServletContext servletContext) {
        String value = servletContext.getInitParameter(JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_INIT_PARAM_NAME);
        if (value != null) {
            return Integer.parseInt(value);
        }
        return JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE;
    }

    private ErrorStrategy getCompoundDocsErrorStrategy(ServletContext servletContext) {
        String value = servletContext.getInitParameter(JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_INIT_PARAM_NAME);
        if (value != null) {
            return ErrorStrategy.valueOf(value);
        }
        return JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE;
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
                                           String rootPath,
                                           ObjectMapper objectMapper,
                                           ExecutorService executorService) {
        DomainRegistry domainRegistry = initDomainRegistry(servletContext);
        OperationsRegistry operationsRegistry = initOperationRegistry(servletContext);
        AccessControlEvaluator accessControlEvaluator = initAccessControlEvaluator(servletContext);
        ErrorHandlerFactoriesRegistry errorHandlerFactory = initErrorHandlerFactory(servletContext);

        ServletRegistration.Dynamic myServlet = servletContext.addServlet(
                JSONAPI4J_DISPATCHER_SERVLET_NAME,
                new JsonApi4jDispatcherServlet(
                        domainRegistry,
                        operationsRegistry,
                        accessControlEvaluator,
                        executorService,
                        errorHandlerFactory,
                        objectMapper
                )
        );
        myServlet.addMapping(rootPath);
    }

    private ExecutorService initExecutorService(ServletContext servletContext) {
        ExecutorService es = (ExecutorService) servletContext.getAttribute(EXECUTOR_SERVICE_ATT_NAME);
        if (es == null) {
            es = Executors.newCachedThreadPool();
        }
        return es;
    }

    private DomainRegistry initDomainRegistry(ServletContext servletContext) {
        DomainRegistry dr = (DomainRegistry) servletContext.getAttribute(DOMAIN_REGISTRY_ATT_NAME);
        if (dr == null) {
            LOG.warn("JsonApiResourceRegistry not found in servlet context. Setting an empty JsonApiResourceRegistry.");
            dr = DomainRegistry.EMPTY;
        }
        return dr;
    }

    private OperationsRegistry initOperationRegistry(ServletContext servletContext) {
        OperationsRegistry or = (OperationsRegistry) servletContext.getAttribute(OPERATION_REGISTRY_ATT_NAME);
        if (or == null) {
            LOG.warn("JsonApiOperationsRegistry not found in servlet context. Setting an empty JsonApiOperationsRegistry.");
            or = OperationsRegistry.EMPTY;
        }
        return or;
    }

    private AccessControlEvaluator initAccessControlEvaluator(ServletContext servletContext) {
        AccessControlEvaluator ace = (AccessControlEvaluator) servletContext.getAttribute(ACCESS_CONTROL_EVALUATOR_ATT_NAME);
        if (ace == null) {
            LOG.warn("AccessControlEvaluator not found in servlet context. Setting a default AccessControlEvaluator.");
            ace = ResourceProcessorContext.DEFAULT_ACCESS_CONTROL_EVALUATOR;
        }
        return ace;
    }

    private ErrorHandlerFactoriesRegistry initErrorHandlerFactory(ServletContext servletContext) {
        ErrorHandlerFactoriesRegistry aehf = (ErrorHandlerFactoriesRegistry) servletContext.getAttribute(ERROR_HANDLER_FACTORY_ATT_NAME);
        if (aehf == null) {
            LOG.warn("AggregatableErrorHandlerFactory not found in servlet context. Setting a default ErrorHandlerFactory.");
            aehf = new JsonApi4jErrorHandlerFactoriesRegistry();
            aehf.registerAll(new DefaultErrorHandlerFactory());
            aehf.registerAll(new Jsr380ErrorHandlers());
        }
        return aehf;
    }

    private ObjectMapper initObjectMapper(ServletContext servletContext) {
        ObjectMapper om = (ObjectMapper) servletContext.getAttribute(OBJECT_MAPPER_ATT_NAME);
        if (om == null) {
            LOG.warn("ObjectMapper not found in servlet context. Setting a default ObjectMapper.");
            om = new ObjectMapper();
            om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            om.registerModule(new JavaTimeModule());
            om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
            om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        }
        return om;
    }

}
