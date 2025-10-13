package io.jsonapi4j.springboot.autoconfiguration.oas;

import io.jsonapi4j.domain.DomainRegistry;
import io.jsonapi4j.operation.OperationsRegistry;
import io.jsonapi4j.springboot.autoconfiguration.SpringJsonApi4JProperties;
import io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers.CommonOpenApiCustomizer;
import io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers.ErrorExamplesCustomizer;
import io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers.JsonApiOperationsCustomizer;
import io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers.JsonApiRequestBodySchemaCustomizer;
import io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers.JsonApiResponseSchemaCustomizer;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@ConditionalOnProperty(
        prefix = "jsonapi4j.oas",
        name = {"enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@Configuration
public class SpringJsonApi4jOasConfig {

    @Bean
    public OpenApiCustomizer openApiCustomizer(
            SpringJsonApi4JProperties properties,
            OperationsRegistry operationsRegistry
    ) {
        return new CommonOpenApiCustomizer(properties.getOas(), operationsRegistry);
    }

    @Bean
    public OpenApiCustomizer jsonApiPathsConfigurer(
            SpringJsonApi4JProperties properties,
            OperationsRegistry operationsRegistry
    ) {
        return new JsonApiOperationsCustomizer(
                properties.getRootPath(),
                operationsRegistry,
                properties.getOas().getCustomResponseHeaders()
        );
    }

    @Bean
    public OpenApiCustomizer jsonApiResponseSchemasConfigurer(
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry) {
        return new JsonApiResponseSchemaCustomizer(domainRegistry, operationsRegistry);
    }

    @Bean
    public OpenApiCustomizer jsonApiRequestBodySchemasConfigurer(
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry) {
        return new JsonApiRequestBodySchemaCustomizer(domainRegistry, operationsRegistry);
    }

    @Bean
    public OpenApiCustomizer errorExamplesCustomizer() {
        return new ErrorExamplesCustomizer();
    }

}
