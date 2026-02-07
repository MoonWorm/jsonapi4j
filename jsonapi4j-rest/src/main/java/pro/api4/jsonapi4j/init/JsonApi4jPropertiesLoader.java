package pro.api4.jsonapi4j.init;

import jakarta.servlet.ServletContext;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.config.JsonApi4jConfigReader;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;

import java.util.Map;

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
            String path = System.getProperty("jsonapi4j.config");
            if (path == null) {
                path = System.getenv("JSONAPI4J_CONFIG");
            }
            if (path == null) {
                path = servletContext.getInitParameter("jsonapi4j.config");
            }
            log.info("Loading configuration from {}", path);
            if (path != null) {
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

    public static Map<String, Object> loadConfigAsMap(ServletContext servletContext) {
        try {
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
