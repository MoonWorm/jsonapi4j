package pro.api4.jsonapi4j.init;

import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;

import java.util.Map;

import static pro.api4.jsonapi4j.init.JsonApi4jServletContainerInitializer.JSONAPI4J_PROPERTIES_ATT_NAME;

@Slf4j
public final class JsonApi4jPropertiesLoader {

    private JsonApi4jPropertiesLoader() {

    }

    /**
     * First, checks if {@link JsonApi4jProperties} is already available as a servlet context attribute
     * {@link JsonApi4jServletContainerInitializer#JSONAPI4J_PROPERTIES_ATT_NAME}.
     * <p>
     * If not found tries to read the config using a path according the next priority:
     * 1. System property (jsonapi4j.config)
     * 2. Environment variable (JSONAPI4J_CONFIG)
     * 3. Servlet Context Init Parameter (jsonapi4j.config)
     * 4. Classpath ("jsonapi4j.yaml" or "jsonapi4j.json")
     *
     * @param servletContext
     * @return
     */
    public static JsonApi4jProperties loadConfig(ServletContext servletContext) {
        try {
            JsonApi4jProperties fromContext = (JsonApi4jProperties) servletContext.getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);
            if (fromContext != null) {
                log.info("Loaded configuration from a servlet context. Attribute: {}", JSONAPI4J_PROPERTIES_ATT_NAME);
                return fromContext;
            }
            String path = System.getProperty("jsonapi4j.config");
            if (path == null) {
                path = System.getenv("JSONAPI4J_CONFIG");
                if (path != null) {
                    log.debug("Got configuration path in a {} System Property.", "jsonapi4j.config");
                }
            }
            if (path == null) {
                path = servletContext.getInitParameter("jsonapi4j.config");
                if (path != null) {
                    log.debug("Got configuration path in a {} Servlet Context Init Parameter.", "jsonapi4j.config");
                }
            }
            if (path != null) {
                log.debug("Loading configuration from {}", path);
                return JsonApi4jConfigReader.readConfig(path);
            }
            return JsonApi4jConfigReader.readConfigFromClasspath(
                    "jsonapi4j.yaml",
                    "jsonapi4j.json"
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JsonApi4jConfig", e);
        }
    }

    public static JsonApi4jProperties loadConfigLenient(ServletContext servletContext) {
        try {
            return loadConfig(servletContext);
        } catch (Exception e) {
            log.warn("Failed to load JsonApi4jConfig. Relying on defaults...");
            return null;
        }
    }

    /**
     * Never checks {@link JsonApi4jServletContainerInitializer#JSONAPI4J_PROPERTIES_ATT_NAME} to avoid missing extra
     * property that can not be deserialized into {@link JsonApi4jProperties}.
     * <p>
     * Reads the config using a path according to the next priority:
     * 1. System property (jsonapi4j.config)
     * 2. Environment variable (JSONAPI4J_CONFIG)
     * 3. Servlet Context Init Parameter (jsonapi4j.config)
     * 4. Classpath ("jsonapi4j.yaml" or "jsonapi4j.json")
     *
     * @param servletContext
     * @return
     */
    public static Map<String, Object> loadConfigAsMap(ServletContext servletContext) {
        try {
            String path = System.getProperty("jsonapi4j.config");
            if (path == null) {
                path = System.getenv("JSONAPI4J_CONFIG");
            }
            if (path == null) {
                path = servletContext.getInitParameter("jsonapi4j.config");
            }
            log.debug("Loading configuration from {}", path);
            if (path != null) {
                return JsonApi4jConfigReader.readConfigAsMap(path);
            }
            return JsonApi4jConfigReader.readConfigFromClasspathAsMap(
                    "jsonapi4j.yaml",
                    "jsonapi4j.json"
            );
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JsonApi4jConfig", e);
        }
    }

}
