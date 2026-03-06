package pro.api4.jsonapi4j.rest.quarkus.runtime;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
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
import pro.api4.jsonapi4j.principal.tier.DefaultAccessTierRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.ErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.JsonApi4jErrorHandlerFactoriesRegistry;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.DefaultErrorHandlerFactory;
import pro.api4.jsonapi4j.servlet.response.errorhandling.impl.Jsr380ErrorHandlers;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Singleton
public class QuarkusJsonApi4jDefaultBeans {

    private static final Logger log = LoggerFactory.getLogger(QuarkusJsonApi4jDefaultBeans.class);

    @Produces
    @Singleton
    @DefaultBean
    ErrorHandlerFactoriesRegistry jsonApi4jErrorHandlerFactoriesRegistry(Instance<ErrorHandlerFactory> customErrorHandlerFactories) {
        log.info("Composing {}...", JsonApi4jErrorHandlerFactoriesRegistry.class.getSimpleName());
        JsonApi4jErrorHandlerFactoriesRegistry jsonapi4jErrorHandlerFactoriesRegistry
                = new JsonApi4jErrorHandlerFactoriesRegistry();
        jsonapi4jErrorHandlerFactoriesRegistry.registerAll(new DefaultErrorHandlerFactory());
        log.info("Default {} error handler factory has been registered", DefaultErrorHandlerFactory.class.getSimpleName());

        jsonapi4jErrorHandlerFactoriesRegistry.registerAll(new Jsr380ErrorHandlers());
        log.info("Default {} error handler factory has been registered", Jsr380ErrorHandlers.class.getSimpleName());

        customErrorHandlerFactories.stream().forEach(f -> {
            jsonapi4jErrorHandlerFactoriesRegistry.registerAll(f);
            log.info("Custom {} error handler factory has been registered", f.getClass().getSimpleName());
        });

        log.info("{} has been successfully composed", JsonApi4jErrorHandlerFactoriesRegistry.class.getSimpleName());
        return jsonapi4jErrorHandlerFactoriesRegistry;
    }

    @Produces
    @Named("jsonApi4jExecutorService")
    @Singleton
    @DefaultBean
    ExecutorService jsonApi4jExecutorService() {
        return Executors.newCachedThreadPool();
    }

    @Produces
    @Singleton
    @DefaultBean
    DomainRegistry domainRegistry(Instance<Resource<?>> resources,
                                  Instance<Relationship<?>> relationships) {
        return DomainRegistry.builder(Collections.emptyList())
                .resources(resources.stream().collect(Collectors.toSet()))
                .relationships(relationships.stream().collect(Collectors.toSet()))
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    OperationsRegistry operationsRegistry(Instance<ResourceOperation> operations) {
        return OperationsRegistry.builder(Collections.emptyList())
                .operations(operations.stream().collect(Collectors.toSet()))
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    JsonApi4j jsonApi4j(DomainRegistry domainRegistry,
                        OperationsRegistry operationsRegistry,
                        @Named("jsonApi4jExecutorService") ExecutorService executorService) {
        List<JsonApi4jPlugin> plugins = Collections.emptyList();
        return JsonApi4j.builder()
                .plugins(plugins)
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .executor(executorService)
                .build();
    }

    @Produces
    @Singleton
    @DefaultBean
    PrincipalResolver principalResolver() {
        return new DefaultPrincipalResolver(new DefaultAccessTierRegistry());
    }

    @Produces
    @Singleton
    @DefaultBean
    ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.registerModule(new JavaTimeModule());
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        mapper.registerModule(new ParameterNamesModule(JsonCreator.Mode.PROPERTIES));
        return mapper;
    }

}
