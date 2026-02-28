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
}
