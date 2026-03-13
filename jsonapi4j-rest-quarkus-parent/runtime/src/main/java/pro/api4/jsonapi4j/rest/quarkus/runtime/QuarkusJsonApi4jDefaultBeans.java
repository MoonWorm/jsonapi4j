package pro.api4.jsonapi4j.rest.quarkus.runtime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperation;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.principal.DefaultPrincipalResolver;
import pro.api4.jsonapi4j.principal.PrincipalResolver;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.Jsr380ErrorHandlers;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@ApplicationScoped
public class QuarkusJsonApi4jDefaultBeans {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jDefaultBeans.class);

    @Produces
    @ApplicationScoped
    @DefaultBean
    ErrorHandlerFactoriesRegistry jsonApi4jErrorHandlerFactoriesRegistry(Instance<ErrorHandlerFactory> customErrorHandlerFactories) {
        LOG.info("Composing {}...", JsonApi4jErrorHandlerFactoriesRegistry.class.getSimpleName());
        JsonApi4jErrorHandlerFactoriesRegistry registry = new JsonApi4jErrorHandlerFactoriesRegistry();

        registry.registerAll(new DefaultErrorHandlerFactory());
        LOG.info("Default {} has been registered", DefaultErrorHandlerFactory.class.getSimpleName());

        registry.registerAll(new Jsr380ErrorHandlers());
        LOG.info("Default {} has been registered", Jsr380ErrorHandlers.class.getSimpleName());

        customErrorHandlerFactories.stream().forEach(f -> {
            registry.registerAll(f);
            LOG.info("Custom {} has been registered", f.getClass().getSimpleName());
        });

        LOG.info("{} has been successfully composed", JsonApi4jErrorHandlerFactoriesRegistry.class.getSimpleName());
        return registry;
    }

    @Produces
    @Named("jsonApi4jExecutorService")
    @ApplicationScoped
    @DefaultBean
    ExecutorService jsonApi4jExecutorService() {
        LOG.info("Composing common {}...", ExecutorService.class.getSimpleName());
        return Executors.newCachedThreadPool();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    AccessTierRegistry accessTierRegistry() {
        LOG.info("Composing {}...", AccessTierRegistry.class.getSimpleName());
        return new DefaultAccessTierRegistry();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    PrincipalResolver principalResolver(AccessTierRegistry accessTierRegistry) {
        LOG.info("Composing {}...", PrincipalResolver.class.getSimpleName());
        return new DefaultPrincipalResolver(accessTierRegistry);
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    List<JsonApi4jPlugin> jsonApi4jPlugins(Instance<JsonApi4jPlugin> plugins) {
        LOG.info("Discovering JsonApi4j plugins...");
        List<JsonApi4jPlugin> result = plugins.stream()
                .sorted(Comparator
                        .comparingInt(JsonApi4jPlugin::precedence)
                        .thenComparing(p -> p.getClass().getName()))
                .toList();
        LOG.info(
                "Discovered {} JsonApi4j plugins: {}",
                result.size(),
                result.stream().map(p -> p.getClass().getSimpleName()).collect(Collectors.joining(", "))
        );
        return result;
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    DomainRegistry domainRegistry(Instance<Resource<?>> resources,
                                  Instance<Relationship<?>> relationships,
                                  List<JsonApi4jPlugin> plugins) {
        Set<Resource<?>> availableResources = resources.stream().collect(Collectors.toSet());
        Set<Relationship<?>> availableRelationships = relationships.stream().collect(Collectors.toSet());
        LOG.info(
                "Composing {}: found {} resources, {} relationships",
                DomainRegistry.class.getSimpleName(),
                availableResources.size(),
                availableRelationships.size()
        );
        return DomainRegistry.builder(plugins)
                .resources(availableResources)
                .relationships(availableRelationships)
                .build();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    OperationsRegistry operationsRegistry(Instance<ResourceOperation> operations,
                                          List<JsonApi4jPlugin> plugins) {
        Set<ResourceOperation> availableOperations = operations.stream().collect(Collectors.toSet());
        LOG.info(
                "Composing {}: found {} operations",
                OperationsRegistry.class.getSimpleName(),
                availableOperations.size()
        );
        return OperationsRegistry.builder(plugins)
                .operations(availableOperations)
                .build();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    JsonApi4j jsonApi4j(DomainRegistry domainRegistry,
                        OperationsRegistry operationsRegistry,
                        List<JsonApi4jPlugin> plugins,
                        @Named("jsonApi4jExecutorService") ExecutorService executorService) {
        LOG.info("Composing {}...", JsonApi4j.class.getSimpleName());
        return JsonApi4j.builder()
                .plugins(plugins)
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .executor(executorService)
                .build();
    }

    @Produces
    @ApplicationScoped
    @DefaultBean
    ObjectMapper objectMapper() {
        LOG.info("Composing common {}...", ObjectMapper.class.getSimpleName());
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        return mapper;
    }

}
