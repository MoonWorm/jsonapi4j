package pro.api4.jsonapi4j.sampleapp.config.swagger;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasDocument;

/**
 * Feeds the JsonApi4j OpenAPI customizers to springdoc.
 * <p>
 * One bean rather than one per customizer, so the framework keeps ownership of the order they are applied in -
 * springdoc would otherwise apply separate {@code OpenApiCustomizer} beans in Spring's collection order, which is
 * not contractually tied to declaration order. Everything each customizer needs comes off the assembled
 * {@link JsonApi4j}.
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
    public OpenApiCustomizer jsonApi4jOpenApiCustomizer(JsonApi4j jsonApi4j) {
        return openApi -> OasDocument.customizers(jsonApi4j).forEach(customizer -> customizer.customise(openApi));
    }

}
