package pro.api4.jsonapi4j.springboot.autoconfiguration.oas;

import org.apache.commons.lang3.StringUtils;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.OasServlet;
import pro.api4.jsonapi4j.springboot.autoconfiguration.SpringJsonApi4JProperties;
import pro.api4.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers.*;

@ConditionalOnProperty(
        prefix = "jsonapi4j.oas",
        name = {"enabled"},
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass(value = {JsonApiOasPlugin.class})
@Configuration
public class SpringJsonApi4jOasPluginConfig {

    @Bean
    public JsonApiOasPlugin jsonApiOasPlugin() {
        return new JsonApiOasPlugin();
    }

    @Bean
    public OpenApiCustomizer openApiCustomizer(
            SpringOasProperties oasProperties,
            OperationsRegistry operationsRegistry
    ) {
        return new CommonOpenApiCustomizer(oasProperties, operationsRegistry);
    }

    @Bean
    public OpenApiCustomizer jsonApiPathsConfigurer(
            SpringJsonApi4JProperties jsonApi4JProperties,
            SpringOasProperties oasProperties,
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry
    ) {
        return new JsonApiOperationsCustomizer(
                jsonApi4JProperties.getRootPath(),
                domainRegistry,
                operationsRegistry,
                oasProperties.getCustomResponseHeaders()
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

    @Bean(name = "jsonApi4jOasServlet")
    public ServletRegistrationBean<?> jsonApi4jOasServlet(
            SpringJsonApi4JProperties jsonApi4jProperties,
            SpringOasProperties oasProperties,
            DomainRegistry domainRegistry,
            OperationsRegistry operationsRegistry
    ) {
        String jsonapi4jRootPath = jsonApi4jProperties.getRootPath();

        String effectiveServletUrlMapping;
        if (StringUtils.isNotBlank(jsonapi4jRootPath) && jsonapi4jRootPath.trim().equals("/")) {
            effectiveServletUrlMapping = "/oas/*";
        } else {
            effectiveServletUrlMapping = jsonapi4jRootPath + "/oas/*";
        }

        ServletRegistrationBean<?> servletRegistration = new ServletRegistrationBean<>(
                new OasServlet(
                        domainRegistry,
                        operationsRegistry,
                        jsonapi4jRootPath,
                        oasProperties
                ),
                effectiveServletUrlMapping
        );
        servletRegistration.setLoadOnStartup(2);
        return servletRegistration;
    }

}
