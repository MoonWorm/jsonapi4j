package pro.api4.jsonapi4j.plugin.oas.init;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.OasServlet;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.servlet.ServletMappings;

import java.util.Map;
import java.util.Set;


@Slf4j
public class JsonApiOasServletContainerInitializer implements ServletContainerInitializer {

    public static final String OAS_PLUGIN_PROPERTIES_ATT_NAME = "jsonApi4jOasPluginProperties";

    public static final String JSONAPI4J_OAS_SERVLET_NAME = "jsonApi4jOasServlet";

    private static OasProperties initOasProperties(ServletContext servletContext) {
        OasProperties oasProperties = (OasProperties) servletContext.getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME);
        if (oasProperties == null) {
            log.warn(
                    "{} are not found in servlet context. Reading from a config file...",
                    OasProperties.class.getSimpleName()
            );
            oasProperties = readOasProperties(servletContext);
            servletContext.setAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME, oasProperties);
        }
        return oasProperties;
    }

    private static OasProperties readOasProperties(ServletContext servletContext) {
        Map<String, Object> jsonApi4jPropertiesRaw = JsonApi4jPropertiesLoader.loadRawConfig(servletContext).getProperties();
        return DefaultOasProperties.toOasProperties(jsonApi4jPropertiesRaw);
    }

    private static void registerOasServlet(ServletContext servletContext, OasProperties oasProperties) {
        ServletRegistration.Dynamic oasServlet = servletContext.addServlet(
                JSONAPI4J_OAS_SERVLET_NAME,
                new OasServlet()
        );
        if (oasServlet == null) {
            log.info(
                    "{} is already registered. Skipping registration.",
                    OasServlet.class.getSimpleName()
            );
            return;
        }
        String servletMapping = ServletMappings.toMapping(oasProperties.oasRootPath());
        log.info(
                "{} is enabled. Registering {} on {} mapping",
                JsonApiOasPlugin.class.getSimpleName(),
                OasServlet.class.getSimpleName(),
                servletMapping
        );
        Set<String> conflictingMappings = oasServlet.addMapping(servletMapping);
        if (!conflictingMappings.isEmpty()) {
            log.warn(
                    "{} could not be mapped on {} - already mapped to a different servlet. The OpenAPI document will"
                            + " not be served on these patterns.",
                    OasServlet.class.getSimpleName(),
                    conflictingMappings
            );
        }
    }

    @Override
    public void onStartup(Set<Class<?>> hooks, ServletContext servletContext) {
        OasProperties oasProperties = initOasProperties(servletContext);
        if (oasProperties.enabled()) {
            registerOasServlet(servletContext, oasProperties);
        } else {
            log.info(
                    "{} is disabled. Not registering {}",
                    JsonApiOasPlugin.class.getSimpleName(),
                    OasServlet.class.getSimpleName()
            );
        }
    }

}
