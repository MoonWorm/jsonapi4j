package pro.api4.jsonapi4j.rest.quarkus.runtime;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.inject.Disposes;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.JsonApiBuildInRequestValidatorFactory;
import pro.api4.jsonapi4j.config.MetaConfigComposer;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.meta.context.MetaRuntime;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperation;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.validation.DefaultJsonApiBuildInRequestValidator;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.config.JsonApi4jProperties.CONFIG_PREFIX;
import static pro.api4.jsonapi4j.config.JsonApi4jProperties.META_PROPERTY;
import static pro.api4.jsonapi4j.config.Integration.QUARKUS;

@Singleton
public class QuarkusJsonApi4jDefaultBeans {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jDefaultBeans.class);

    @Produces
    @Singleton
    @DefaultBean
    ErrorHandlerFactoriesRegistry jsonApi4jErrorHandlerFactoriesRegistry(Instance<ErrorHandlerFactory> customErrorHandlerFactories) {
        LOG.info("Composing {}...", JsonApi4jErrorHandlerFactoriesRegistry.class.getSimpleName());
        JsonApi4jErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();

        registry.registerAll(new DefaultErrorHandlerFactory());
        LOG.info("Default {} has been registered", DefaultErrorHandlerFactory.class.getSimpleName());

        customErrorHandlerFactories.stream().forEach(f -> {
            registry.registerAll(f);
            LOG.info("Custom {} has been registered", f.getClass().getSimpleName());
        });

        LOG.info("{} has been successfully composed", JsonApi4jErrorHandlerFactoriesRegistry.class.getSimpleName());
        return registry;
    }

    @Produces
    @Named("jsonApi4jExecutorService")
    @Singleton
    @DefaultBean
    ExecutorService jsonApi4jExecutorService() {
        LOG.info("Composing common {}...", ExecutorService.class.getSimpleName());
        return Executors.newCachedThreadPool();
    }

    /**
     * Disposes of the pool produced above. The disposer belongs to that producer, so an application that supplies
     * its own {@code ExecutorService} replaces the {@link DefaultBean} and this never runs - it can only ever shut
     * down a pool the framework created.
     * <p>
     * Matters most in dev mode, where a live reload restarts the application without restarting the JVM and would
     * otherwise strand a cached thread pool on every reload.
     */
    void disposeJsonApi4jExecutorService(@Disposes @Named("jsonApi4jExecutorService") ExecutorService executorService) {
        LOG.info("Shutting down common {}...", ExecutorService.class.getSimpleName());
        executorService.shutdown();
    }

    @Produces
    @Singleton
    @DefaultBean
    JsonApiBuildInRequestValidatorFactory jsonApi4jValidatorFactory(QuarkusJsonApi4jProperties properties,
                                                                    @Named("jsonApi4jObjectMapper") ObjectMapper objectMapper) {
        LOG.info("Composing JsonApi4jValidatorFactory {}...", JsonApiBuildInRequestValidatorFactory.class.getSimpleName());
        return domainRegistry -> DefaultJsonApiBuildInRequestValidator.builder()
                .properties(properties.validation().map(QuarkusJsonApi4jProperties.QuarkusValidationProperties::toJsonApi4jProperties).orElse(null))
                .objectMapper(objectMapper)
                .domainRegistry(domainRegistry)
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    PrincipalResolver principalResolver() {
        LOG.info("Composing {}...", PrincipalResolver.class.getSimpleName());
        return new DefaultPrincipalResolver();
    }

    @Produces
    @Singleton
    @DefaultBean
    PluginRegistry pluginRegistry(Instance<JsonApi4jPlugin> plugins) {
        LOG.info("Discovering JsonApi4j plugins...");
        PluginRegistry pluginRegistry = PluginRegistry.builder().registerAll(plugins.stream().toList()).build();
        LOG.info(
                "Discovered {} JsonApi4j plugins: {}",
                pluginRegistry.getAllPlugins().size(),
                pluginRegistry.getAllPlugins().stream().map(p -> p.getClass().getSimpleName()).collect(Collectors.joining(", "))
        );
        return pluginRegistry;
    }

    @IfBuildProperty(name = CONFIG_PREFIX + "." + META_PROPERTY + ".enabled", stringValue = "true")
    @Produces
    @Singleton
    @DefaultBean
    MetaContext jsonApi4jMetaContext(PluginRegistry pluginRegistry, QuarkusJsonApi4jProperties rootProperties) {
        MetaContext context = MetaContext.of(
                MetaConfigComposer.compose(rootProperties.toJsonApi4jProperties(), pluginRegistry),
                QUARKUS
        );
        LOG.info("Composing {} (meta API enabled)", MetaRuntime.class.getSimpleName());
        return context;
    }

    @Produces
    @Singleton
    @DefaultBean
    DomainRegistry domainRegistry(Instance<Resource<?>> resources,
                                  Instance<Relationship<?>> relationships,
                                  PluginRegistry pluginRegistry) {
        Set<Resource<?>> availableResources = resources.stream().collect(Collectors.toSet());
        Set<Relationship<?>> availableRelationships = relationships.stream().collect(Collectors.toSet());
        LOG.info(
                "Composing {}: found {} resources, {} relationships",
                DomainRegistry.class.getSimpleName(),
                availableResources.size(),
                availableRelationships.size()
        );
        return DomainRegistry.builder(pluginRegistry)
                .resources(availableResources)
                .relationships(availableRelationships)
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    OperationsRegistry operationsRegistry(Instance<ResourceOperation> operations,
                                          PluginRegistry pluginRegistry) {
        Set<ResourceOperation> availableOperations = operations.stream().collect(Collectors.toSet());
        LOG.info(
                "Composing {}: found {} operations",
                OperationsRegistry.class.getSimpleName(),
                availableOperations.size()
        );
        return OperationsRegistry.builder(pluginRegistry)
                .operations(availableOperations)
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    JsonApi4j jsonApi4j(DomainRegistry domainRegistry,
                        OperationsRegistry operationsRegistry,
                        PluginRegistry pluginRegistry,
                        @Named("jsonApi4jExecutorService") ExecutorService executorService,
                        JsonApiBuildInRequestValidatorFactory validatorFactory,
                        Instance<MetaContext> metaContext,
                        QuarkusJsonApi4jProperties rootProperties) {
        LOG.info("Composing {}...", JsonApi4j.class.getSimpleName());
        return JsonApi4j.builder()
                .properties(rootProperties.toJsonApi4jProperties())
                .pluginRegistry(pluginRegistry)
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .executor(executorService)
                .validatorFactory(validatorFactory)
                .meta(metaContext.isResolvable() ? metaContext.get() : null)
                .build();
    }

    @Produces
    @Named("jsonApi4jObjectMapper")
    @Singleton
    @DefaultBean
    ObjectMapper objectMapper() {
        LOG.info("Composing common {}...", ObjectMapper.class.getSimpleName());
        return JsonApi4jServletContainerInitializer.createObjectMapper();
    }

}
