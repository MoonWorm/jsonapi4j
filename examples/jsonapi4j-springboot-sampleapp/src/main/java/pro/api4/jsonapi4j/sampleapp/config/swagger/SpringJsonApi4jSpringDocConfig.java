package pro.api4.jsonapi4j.sampleapp.config.swagger;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.customizer.*;

/**
 * Feeds the JsonApi4j OpenAPI customizers to springdoc.
 * <p>
 * Each customizer takes the assembled {@link JsonApi4j} and reads everything it needs from it - the registries, the
 * root configuration and the OAS plugin's own configuration. springdoc's {@code OpenApiCustomizer} declares the
 * same {@code customise(OpenAPI)} method, so a method reference is the whole adapter.
 */
@ConditionalOnProperty(
        prefix = "jsonapi4j.oas",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass(value = {JsonApiOasPlugin.class})
@Configuration
public class SpringJsonApi4jSpringDocConfig {

    @Bean
    public OpenApiCustomizer openApiCustomizer(JsonApi4j jsonApi4j) {
        return new CommonOpenApiCustomizer(jsonApi4j)::customise;
    }

    @Bean
    public OpenApiCustomizer jsonApiResponseSchemasConfigurer(JsonApi4j jsonApi4j) {
        return new JsonApiResponseSchemaCustomizer(jsonApi4j)::customise;
    }

    @Bean
    public OpenApiCustomizer jsonApiRequestBodySchemasConfigurer(JsonApi4j jsonApi4j) {
        return new JsonApiRequestBodySchemaCustomizer(jsonApi4j)::customise;
    }

    @Bean
    public OpenApiCustomizer jsonApiPathsConfigurer(JsonApi4j jsonApi4j) {
        return new JsonApiOperationsCustomizer(jsonApi4j)::customise;
    }

    @Bean
    public OpenApiCustomizer errorExamplesCustomizer() {
        return new ErrorExamplesCustomizer()::customise;
    }

}
