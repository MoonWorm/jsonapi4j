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
     * Priority:
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
                    log.info("Got configuration path in a {} System Property.", "jsonapi4j.config");
                }
            }
            if (path == null) {
                path = servletContext.getInitParameter("jsonapi4j.config");
                if (path != null) {
                    log.info("Got configuration path in a {} Servlet Context Init Parameter.", "jsonapi4j.config");
                }
            }
            if (path != null) {
                log.info("Loading configuration from {}", path);
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

    public static Map<String, Object> loadConfigAsMap(ServletContext servletContext) {
        try {
            JsonApi4jProperties fromContext = (JsonApi4jProperties) servletContext.getAttribute(JSONAPI4J_PROPERTIES_ATT_NAME);
            if (fromContext != null) {
                return JsonApi4jConfigReader.readConfigFromClasspathAsMap(fromContext);
            }
            String path = System.getProperty("jsonapi4j.config");
            if (path == null) {
                path = System.getenv("JSONAPI4J_CONFIG");
            }
            if (path == null) {
                path = servletContext.getInitParameter("jsonapi4j.config");
            }
            log.info("Loading configuration from {}", path);
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
