package pro.api4.jsonapi4j.springboot.autoconfiguration.oas;

import org.apache.commons.lang3.StringUtils;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.OasServlet;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_PROPERTIES_ATT_NAME;
import static pro.api4.jsonapi4j.plugin.oas.init.JsonApiOasServletContainerInitializer.OAS_PLUGIN_ROOT_PATH_ATT_NAME;

@ConditionalOnProperty(
        prefix = "jsonapi4j.oas",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = true
)
@ConditionalOnClass(value = {JsonApiOasPlugin.class})
@EnableConfigurationProperties(SpringOasProperties.class)
@Configuration
public class SpringJsonApi4jOasPluginConfig {

    @Bean
    public JsonApiOasPlugin jsonApiOasPlugin(SpringOasProperties oasProperties) {
        return new JsonApiOasPlugin(oasProperties);
    }

    @Bean
    public ServletContextInitializer jsonApi4jOasServletContextInitializer(
            JsonApi4jProperties jsonApi4jProperties,
            OasProperties oasProperties
    ) {
        return servletContext -> {
            servletContext.setAttribute(OAS_PLUGIN_ROOT_PATH_ATT_NAME, jsonApi4jProperties.rootPath());
            servletContext.setAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME, oasProperties);
        };
    }

    @Bean(name = "jsonApi4jOasServlet")
    public ServletRegistrationBean<?> jsonApi4jOasServlet(
            JsonApi4jProperties jsonApi4jProperties
    ) {
        String jsonapi4jRootPath = jsonApi4jProperties.rootPath();

        String effectiveServletUrlMapping;
        if (StringUtils.isNotBlank(jsonapi4jRootPath) && jsonapi4jRootPath.trim().equals("/")) {
            effectiveServletUrlMapping = "/oas/*";
        } else {
            effectiveServletUrlMapping = jsonapi4jRootPath + "/oas/*";
        }

        ServletRegistrationBean<?> servletRegistration = new ServletRegistrationBean<>(
                new OasServlet(),
                effectiveServletUrlMapping
        );
        servletRegistration.setLoadOnStartup(2);
        return servletRegistration;
    }

}
