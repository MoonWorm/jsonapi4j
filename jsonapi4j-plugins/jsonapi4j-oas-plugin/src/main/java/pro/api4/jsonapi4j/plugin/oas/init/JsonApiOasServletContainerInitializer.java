package pro.api4.jsonapi4j.plugin.oas.init;

import jakarta.servlet.ServletContainerInitializer;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletRegistration;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.init.JsonApi4jPropertiesLoader;
import pro.api4.jsonapi4j.plugin.oas.OasServlet;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import java.util.Collections;
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

    private void initOasProperties(ServletContext servletContext) {
        OasProperties oasProperties = (OasProperties) servletContext.getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME);
        if (oasProperties == null) {
            log.warn("Oas Properties are not found in servlet context. Reading from a config file...");
            oasProperties = readOasProperties(servletContext);
            servletContext.setAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME, oasProperties);
        }
    }

    private OasProperties readOasProperties(ServletContext servletContext) {
        Map<String, Object> jsonApi4jPropertiesRaw = JsonApi4jPropertiesLoader.loadConfigAsMap(servletContext);
        Object oasPropertiesObject = jsonApi4jPropertiesRaw.get(OasProperties.OAS_PROPERTY_NAME);
        Map<String, Object> oasPropertiesRaw = Collections.emptyMap();
        if (oasPropertiesObject instanceof Map oasPropertiesMap) {
            //noinspection unchecked
            oasPropertiesRaw = oasPropertiesMap;
        }

        OasProperties oasProperties = new OasProperties();
        if (!oasPropertiesRaw.isEmpty()) {
            oasProperties = JsonApi4jConfigReader.convertToConfig(
                    oasPropertiesRaw,
                    OasProperties.class
            );
        }
        return oasProperties;
    }

    private void initRootPath(ServletContext servletContext) {
        if (servletContext.getInitParameter(OAS_PLUGIN_ROOT_PATH_ATT_NAME) == null) {
            log.warn("Oas Root Path attribute is not set in servlet context. Reading from a JsonApi4j config file...");
            servletContext.setAttribute(
                    OAS_PLUGIN_ROOT_PATH_ATT_NAME,
                    readJsonApi4jProperties(servletContext).getRootPath()
            );
        }
    }

    private JsonApi4jProperties readJsonApi4jProperties(ServletContext servletContext) {
        Map<String, Object> rawJsonApi4jProperties = JsonApi4jPropertiesLoader.loadConfigAsMap(servletContext);
        return JsonApi4jConfigReader.convertToConfig(rawJsonApi4jProperties, JsonApi4jProperties.class);
    }

    private void registerOasServlet(ServletContext servletContext) {
        ServletRegistration.Dynamic oasServlet = servletContext.addServlet(
                JSONAPI4J_OAS_SERVLET_NAME,
                new OasServlet()
        );

        OasProperties oasProperties = (OasProperties) servletContext.getAttribute(OAS_PLUGIN_PROPERTIES_ATT_NAME);
        String servletMapping = oasProperties.getOasRootPath() + "/*";
        log.info("Registering OAS Servlet on {} mapping", servletMapping);
        oasServlet.addMapping(servletMapping);
    }

}
