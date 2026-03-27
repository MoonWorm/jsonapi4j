package pro.api4.jsonapi4j.plugin.oas.init;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.plugin.oas.OasServlet;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import java.util.Map;
import java.util.Set;

@Slf4j
public class JsonApiOasServletContainerInitializer implements ServletContainerInitializer {

    public static final String OAS_PLUGIN_ROOT_PATH_ATT_NAME = "jsonApi4jOasPluginRootPath";
    public static final String OAS_PLUGIN_PROPERTIES_ATT_NAME = "jsonApi4jOasPluginProperties";

    public static final String JSONAPI4J_OAS_SERVLET_NAME = "jsonApi4jOasServlet";

    @Override
    public void onStartup(Set<Class<?>> hooks, ServletContext servletContext) {
        initRootPath(servletContext);
        initOasProperties(servletContext);
        registerOasServlet(servletContext);
    }

    private void initRootPath(ServletContext servletContext) {
        if (servletContext.getInitParameter(OAS_PLUGIN_ROOT_PATH_ATT_NAME) == null) {
            log.warn("Oas Root Path attribute is not set in servlet context. Reading from a JsonApi4j config file...");
            servletContext.setAttribute(
                    OAS_PLUGIN_ROOT_PATH_ATT_NAME,
                    readJsonApi4jProperties(servletContext).rootPath()
            );
        }
    }

    private JsonApi4jProperties readJsonApi4jProperties(ServletContext servletContext) {
        return JsonApi4jPropertiesLoader.loadConfig(servletContext);
    }

    private void initOasProperties(ServletContext servletContext) {
        OasProperties oasProperties = (OasProperties) servletContext.getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME);
        if (oasProperties == null) {
            log.warn("Oas Properties are not found in servlet context. Reading from a config file...");
            oasProperties = readOasProperties(servletContext);
            servletContext.setAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME, oasProperties);
        }
    }

    private static OasProperties readOasProperties(ServletContext servletContext) {
        Map<String, Object> jsonApi4jPropertiesRaw = JsonApi4jPropertiesLoader.loadConfigAsMap(servletContext);
        return DefaultOasProperties.toOasProperties(jsonApi4jPropertiesRaw);
    }

    private void registerOasServlet(ServletContext servletContext) {
        OasProperties oasProperties = (OasProperties) servletContext.getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME);
        if (oasProperties.enabled()) {
            ServletRegistration.Dynamic oasServlet = servletContext.addServlet(
                    JSONAPI4J_OAS_SERVLET_NAME,
                    new OasServlet()
            );
            String servletMapping = oasProperties.oasRootPath() + "/*";
            log.info("OAS Plugin is enabled. Registering OAS Servlet on {} mapping", servletMapping);
            oasServlet.addMapping(servletMapping);
        } else {
            log.info("OAS Plugin is disabled. Not registering OAS Servlet");
        }
    }

}
