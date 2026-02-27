package pro.api4.jsonapi4j.config;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class JsonApi4jProperties {

    public static final String ROOT_PATH_PROPERTY_NAME = "rootPath";

    public static final String JSONAPI4J_DEFAULT_ROOT_PATH = "/jsonapi";

    private String rootPath = JSONAPI4J_DEFAULT_ROOT_PATH;
    private CompoundDocsProperties compoundDocs = new CompoundDocsProperties();
    private CompatibilityProperties compatibility = new CompatibilityProperties();
    private ExecutorProperties executor = new ExecutorProperties();

    public CompatibilityProperties getCompatibility() {
        if (compatibility == null) {
            compatibility = new CompatibilityProperties();
        }
        return compatibility;
    }

    public ExecutorProperties getExecutor() {
        if (executor == null) {
            executor = new ExecutorProperties();
        }
        return executor;
    }
}
