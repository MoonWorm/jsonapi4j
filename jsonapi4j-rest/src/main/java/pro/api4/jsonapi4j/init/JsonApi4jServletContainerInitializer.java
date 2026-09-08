package pro.api4.jsonapi4j.init;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.UnavailableException;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.JsonApiBuildInRequestValidatorFactory;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.filter.principal.PrincipalResolvingFilter;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.servlet.JsonApi4jDispatcherServlet;
import pro.api4.jsonapi4j.servlet.request.body.RequestBodyCachingFilter;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.validation.DefaultJsonApiBuildInRequestValidator;

import java.util.Collection;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader.loadConfigLenient;
import pro.api4.jsonapi4j.servlet.ServletMappings;

@Slf4j
public class JsonApi4jServletContainerInitializer implements ServletContainerInitializer {

    public static final String JSONAPI4J_DISPATCHER_SERVLET_NAME = "jsonApi4jDispatcherServlet";
    public static final String JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME = "jsonapi4jPrincipalResolvingFilter";
    public static final String JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME = "jsonapi4jRequestBodyCachingFilter";

    public static final String JSONAPI4J_PROPERTIES_ATT_NAME = "jsonApi4jProperties";
    public static final String JSONAPI4J_ATT_NAME = "jsonApi4j";
    public static final String EXECUTOR_SERVICE_ATT_NAME = "jsonApi4jExecutorService";
    public static final String VALIDATOR_FACTORY_ATT_NAME = "jsonApi4jValidatorFactory";

    /**
     * Inputs a servlet host sets <em>before</em> startup to hand the framework its own pieces:
     * {@link #DOMAIN_REGISTRY_ATT_NAME}, {@link #OPERATION_REGISTRY_ATT_NAME}, {@link #PLUGIN_REGISTRY_ATT_NAME},
     * {@link #META_CONTEXT_ATT_NAME} and {@link #VALIDATOR_FACTORY_ATT_NAME}. Each is read once while
     * {@code JsonApi4j} is assembled and never again - afterwards everything they carry is reachable from the
     * assembled instance, which lives under {@link #JSONAPI4J_ATT_NAME}. Absent ones fall back to a default that
     * is used but not stored back.
     */
    public static final String DOMAIN_REGISTRY_ATT_NAME = "jsonapi4jDomainRegistry";
    public static final String OPERATION_REGISTRY_ATT_NAME = "jsonapi4jOperationRegistry";
    public static final String PLUGIN_REGISTRY_ATT_NAME = "jsonapi4jPluginRegistry";
    public static final String ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME = "jsonapi4jErrorHandlerFactoriesRegistry";
    public static final String OBJECT_MAPPER_ATT_NAME = "jsonApi4jObjectMapper";
    public static final String PRINCIPAL_RESOLVER_ATT_NAME = "jsonapi4jPrincipalResolver";
    public static final String META_CONTEXT_ATT_NAME = "jsonapi4jMetaContext";

    public static ObjectMapper initObjectMapper(ServletContext servletContext) {
        ObjectMapper om = (ObjectMapper) servletContext.getAttribute(OBJECT_MAPPER_ATT_NAME);
        if (om == null) {
            log.warn("ObjectMapper not found in servlet context. Setting a default ObjectMapper.");
            om = createObjectMapper();
            servletContext.setAttribute(OBJECT_MAPPER_ATT_NAME, om);
        }
        return om;
    }

    public static ObjectMapper createObjectMapper() {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        om.registerModule(new JavaTimeModule());
        om.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        om.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        om.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        SimpleModule relationshipModule = new SimpleModule();
        relationshipModule.addDeserializer(RelationshipObject.class, new RelationshipObjectDeserializer());
        om.registerModule(relationshipModule);
        return om;
    }

    public static JsonApi4jProperties initJsonApi4jProperties(ServletContext servletContext) {
        JsonApi4jProperties properties = (JsonApi4jProperties) servletContext.getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);
        if (properties == null) {
            properties = loadConfigLenient(servletContext);
            servletContext.setAttribute(JSONAPI4J_PROPERTIES_ATT_NAME, properties);
        }
        return properties;
    }

    public static ExecutorService initExecutorService(ServletContext servletContext) {
        ExecutorService es = (ExecutorService) servletContext.getAttribute(EXECUTOR_SERVICE_ATT_NAME);
        if (es == null) {
            log.warn("Executor not found in servlet context. Setting a default one (Executors.newCachedThreadPool).");
            es = Executors.newCachedThreadPool();
            servletContext.setAttribute(EXECUTOR_SERVICE_ATT_NAME, es);
        }
        return es;
    }

    private static JsonApiBuildInRequestValidatorFactory initValidatorFactory(ServletContext servletContext) {
        JsonApiBuildInRequestValidatorFactory factory =
                (JsonApiBuildInRequestValidatorFactory) servletContext.getAttribute(VALIDATOR_FACTORY_ATT_NAME);
        if (factory != null) {
            return factory;
        }
        log.warn("JsonApi4jValidatorFactory not found in servlet context. Setting a default one ({}).", DefaultJsonApiBuildInRequestValidator.class.getSimpleName());
        ObjectMapper objectMapper = initObjectMapper(servletContext);
        JsonApi4jProperties properties = initJsonApi4jProperties(servletContext);
        factory = domainRegistry -> DefaultJsonApiBuildInRequestValidator.builder()
                .objectMapper(objectMapper)
                .properties(properties.validation())
                .domainRegistry(domainRegistry)
                .build();
        return factory;
    }

    private static DomainRegistry initDomainRegistry(ServletContext servletContext) {
        DomainRegistry dr = (DomainRegistry) servletContext.getAttribute(DOMAIN_REGISTRY_ATT_NAME);
        if (dr == null) {
            log.warn("DomainRegistry not found in servlet context. Using an empty DomainRegistry.");
            dr = DomainRegistry.empty();
        }
        return dr;
    }

    private static OperationsRegistry initOperationRegistry(ServletContext servletContext) {
        OperationsRegistry or = (OperationsRegistry) servletContext.getAttribute(OPERATION_REGISTRY_ATT_NAME);
        if (or == null) {
            log.warn("JsonApiOperationsRegistry not found in servlet context. Using an empty JsonApiOperationsRegistry.");
            or = OperationsRegistry.empty();
        }
        return or;
    }

    private static ErrorHandlerFactoriesRegistry initErrorHandlerFactory(ServletContext context) {
        ErrorHandlerFactoriesRegistry errorHandlerFactoriesRegistry
                = (ErrorHandlerFactoriesRegistry) context.getAttribute(ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME);
        if (errorHandlerFactoriesRegistry == null) {
            log.warn("AggregatableErrorHandlerFactory not found in servlet context. Applying a default ErrorHandlerFactory.");
            errorHandlerFactoriesRegistry = new JsonApi4jErrorHandlerFactoriesRegistry();
            errorHandlerFactoriesRegistry.registerAll(new DefaultErrorHandlerFactory());
            context.setAttribute(ERROR_HANDLER_FACTORIES_REGISTRY_ATT_NAME, errorHandlerFactoriesRegistry);
        }
        return errorHandlerFactoriesRegistry;
    }

    private static PrincipalResolver initPrincipalResolver(ServletContext servletContext) {
        PrincipalResolver principalResolver = (PrincipalResolver) servletContext.getAttribute(PRINCIPAL_RESOLVER_ATT_NAME);
        if (principalResolver == null) {
            log.warn(
                    "{} not found in servlet context. Applying a default one ({}).",
                    PrincipalResolver.class.getSimpleName(),
                    DefaultPrincipalResolver.class.getSimpleName()
            );
            principalResolver = new DefaultPrincipalResolver();
            servletContext.setAttribute(PRINCIPAL_RESOLVER_ATT_NAME, principalResolver);
        }
        return principalResolver;
    }

    private static PluginRegistry initPluginRegistry(ServletContext servletContext) {
        PluginRegistry plugins = (PluginRegistry) servletContext.getAttribute(PLUGIN_REGISTRY_ATT_NAME);
        if (plugins == null) {
            log.warn("{} not found in servlet context. Using an empty one.", PluginRegistry.class.getSimpleName());
            plugins = PluginRegistry.empty();
        }
        return plugins;
    }

    /**
     * Returns the assembled {@link JsonApi4j}, composing it from the servlet context on first use and caching it under
     * {@link #JSONAPI4J_ATT_NAME}.
     *
     * <p>Composition is deliberately deferred to servlet initialization rather than performed here during
     * {@code onStartup}. A container that discovers this initializer itself - a war deployed to an external servlet
     * container - calls {@code onStartup} before any application code can contribute its registries, because
     * {@code ServletContextListener.contextInitialized} fires only after every initializer has run. Composing eagerly
     * would cache an empty instance that later contributions could never replace. Composing on first read leaves that
     * window open, so a listener can supply {@link #DOMAIN_REGISTRY_ATT_NAME} and friends the way the Quarkus
     * integration already does.
     */
    public static JsonApi4j initJsonApi4j(ServletContext servletContext) throws ServletException {
        JsonApi4j jsonApi4j = (JsonApi4j) servletContext.getAttribute(JSONAPI4J_ATT_NAME);
        if (jsonApi4j == null) {
            log.warn("JsonApi4j not found in servlet context. Trying to compose an instance.");
            DomainRegistry domainRegistry = initDomainRegistry(servletContext);
            OperationsRegistry operationsRegistry = initOperationRegistry(servletContext);
            PluginRegistry plugins = initPluginRegistry(servletContext);
            ExecutorService executorService = initExecutorService(servletContext);
            JsonApiBuildInRequestValidatorFactory validatorFactory = initValidatorFactory(servletContext);
            // if meta context is null = meta feature is disabled
            MetaContext metaContext = (MetaContext) servletContext.getAttribute(META_CONTEXT_ATT_NAME);
            jsonApi4j = JsonApi4j.builder()
                    .properties(initJsonApi4jProperties(servletContext))
                    .domainRegistry(domainRegistry)
                    .operationsRegistry(operationsRegistry)
                    .pluginRegistry(plugins)
                    .executor(executorService)
                    .validatorFactory(validatorFactory)
                    .meta(metaContext)
                    .build();
            servletContext.setAttribute(JSONAPI4J_ATT_NAME, jsonApi4j);
        }
        verifyRootPathMatchesMapping(servletContext, jsonApi4j);
        return jsonApi4j;
    }

    /**
     * Fails when the configured {@code rootPath} disagrees with the mapping the dispatcher servlet is registered
     * under.
     * <p>
     * The mapping is computed from {@code rootPath} while this initializer runs and is immutable afterwards, but
     * {@code rootPath} is read again for {@code self} and {@code related} links, the Meta API path templates and
     * Compound Docs prefix stripping. Configuration arriving after deployment therefore cannot move the endpoints
     * yet changes every link the API reports about itself, and requests keep succeeding because routing follows the
     * mapping and parsing uses {@code getPathInfo()}.
     * <p>
     * Checked here rather than in the dispatcher servlet because the dispatcher initializes <em>last</em>: filters
     * and the OpenAPI servlet are already running by then, and the OpenAPI servlet is what triggers composition in
     * the first place. Guarding at composition makes every consumer fail together instead of leaving the OpenAPI
     * document served on a path whose endpoints do not exist.
     */
    private static void verifyRootPathMatchesMapping(ServletContext servletContext, JsonApi4j jsonApi4j)
            throws ServletException {
        ServletRegistration registration = servletContext.getServletRegistration(JSONAPI4J_DISPATCHER_SERVLET_NAME);
        if (registration == null) {
            return;
        }
        String expectedMapping = ServletMappings.toMapping(jsonApi4j.getProperties().rootPath());
        Collection<String> actualMappings = registration.getMappings();
        if (actualMappings.isEmpty() || actualMappings.contains(expectedMapping)) {
            return;
        }
        String message = String.format(
                "%s is mapped on %s but the configured rootPath resolves to %s. The mapping is fixed while the"
                        + " container initializer runs, so configuration supplied after deployment cannot move it -"
                        + " every link the API generates would point at %s, where nothing is mapped. Configure"
                        + " rootPath through the config file, the jsonapi4j.config system property, the"
                        + " JSONAPI4J_CONFIG environment variable or a servlet context init parameter.",
                JSONAPI4J_DISPATCHER_SERVLET_NAME,
                String.join(", ", actualMappings),
                expectedMapping,
                expectedMapping
        );
        // Logged as well as thrown: a container is free to swallow the exception and simply mark the servlet
        // unavailable, which leaves an operator with an unexplained 404 unless the reason was written down here.
        log.error(message);
        throw new UnavailableException(message);
    }

    @Override
    public void onStartup(Set<Class<?>> hooks, ServletContext servletContext) {
        // ------------------
        // init
        // ------------------
        JsonApi4jProperties properties = initJsonApi4jProperties(servletContext);
        initObjectMapper(servletContext);
        initErrorHandlerFactory(servletContext);
        initPrincipalResolver(servletContext);

        // ------------------
        // dispatcher servlet
        // ------------------
        String dispatcherServletMapping = ServletMappings.toMapping(properties.rootPath());
        registerDispatcherServlet(
                servletContext,
                dispatcherServletMapping
        );

        // -------
        // filters
        // -------
        registerPrincipalResolvingFilter(servletContext, dispatcherServletMapping);

        registerRequestBodyCachingFilter(servletContext, dispatcherServletMapping);
    }

    private void registerDispatcherServlet(ServletContext servletContext,
                                           String servletMapping) {
        ServletRegistration.Dynamic dispatcherServlet = servletContext.addServlet(
                JSONAPI4J_DISPATCHER_SERVLET_NAME,
                new JsonApi4jDispatcherServlet()
        );
        if (dispatcherServlet == null) {
            log.info(
                    "{} is already registered. Skipping registration.",
                    JsonApi4jDispatcherServlet.class.getSimpleName()
            );
            return;
        }
        dispatcherServlet.setLoadOnStartup(1);
        Set<String> conflictingMappings = dispatcherServlet.addMapping(servletMapping);
        if (!conflictingMappings.isEmpty()) {
            log.warn(
                    "{} could not be mapped on {} - already mapped to a different servlet. Requests to these patterns"
                            + " will not reach JsonApi4j.",
                    JsonApi4jDispatcherServlet.class.getSimpleName(),
                    conflictingMappings
            );
            return;
        }
        log.info("{} has been successfully registered under {} root path", JsonApi4jDispatcherServlet.class.getSimpleName(), servletMapping);
    }

    private void registerPrincipalResolvingFilter(ServletContext servletContext, String rootPath) {
        FilterRegistration.Dynamic filter = servletContext.addFilter(
                JSONAPI4J_PRINCIPAL_RESOLVING_FILTER_NAME,
                new PrincipalResolvingFilter()
        );
        if (filter == null) {
            log.info(
                    "{} is already registered. Skipping registration.",
                    PrincipalResolvingFilter.class.getSimpleName()
            );
            return;
        }
        filter.addMappingForUrlPatterns(
                null, // DispatcherType.REQUEST is used by default
                false, // supposed to be matched before any declared filter mappings of the ServletContext
                rootPath
        );
    }

    private void registerRequestBodyCachingFilter(ServletContext servletContext, String rootPath) {
        FilterRegistration.Dynamic filter = servletContext.addFilter(
                JSONAPI4J_REQUEST_BODY_CACHING_FILTER_NAME,
                new RequestBodyCachingFilter()
        );
        if (filter == null) {
            log.info(
                    "{} is already registered. Skipping registration.",
                    RequestBodyCachingFilter.class.getSimpleName()
            );
            return;
        }
        filter.addMappingForUrlPatterns(
                null, // DispatcherType.REQUEST is used by default
                false, // supposed to be matched before any declared filter mappings of the ServletContext
                rootPath
        );
    }

}
