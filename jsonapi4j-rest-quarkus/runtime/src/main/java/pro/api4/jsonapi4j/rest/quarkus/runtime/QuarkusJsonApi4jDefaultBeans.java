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

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

@Singleton
public class QuarkusJsonApi4jDefaultBeans {

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
