package pro.api4.jsonapi4j.sampleapp.config.swagger;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.sampleapp.config.swagger.customizers.*;

@ConditionalOnProperty(
        prefix = "jsonapi4j.oas",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass(value = { JsonApiOasPlugin.class })
@Configuration
public class SpringJsonApi4jSpringDocConfig {

    @Bean
    public OpenApiCustomizer openApiCustomizer(
            OasProperties oasProperties,
            OperationsRegistry operationsRegistry
    ) {
        return new CommonOpenApiCustomizer(oasProperties, operationsRegistry);
    }

    @Bean
    public OpenApiCustomizer jsonApiPathsConfigurer(
            JsonApi4jProperties jsonApi4JProperties,
            OasProperties oasProperties,
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry
    ) {
        return new JsonApiOperationsCustomizer(
                jsonApi4JProperties.rootPath(),
                domainRegistry,
                operationsRegistry,
                oasProperties.customResponseHeaders()
        );
    }

    @Bean
    public OpenApiCustomizer jsonApiResponseSchemasConfigurer(
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry) {
        return new JsonApiResponseSchemaCustomizer(domainRegistry, operationsRegistry);
    }

    @Bean
    public OpenApiCustomizer jsonApiRequestBodySchemasConfigurer(OperationsRegistry operationsRegistry) {
        return new JsonApiRequestBodySchemaCustomizer(operationsRegistry);
    }

    @Bean
    public OpenApiCustomizer errorExamplesCustomizer() {
        return new ErrorExamplesCustomizer();
    }


}
