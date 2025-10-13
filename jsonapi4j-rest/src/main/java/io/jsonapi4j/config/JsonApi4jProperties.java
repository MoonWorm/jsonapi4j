package io.jsonapi4j.config;

import lombok.Data;

@Data
public class JsonApi4jProperties {

    public static final String JSONAPI4J_DEFAULT_ROOT_PATH = "/jsonapi";

    private String rootPath = JSONAPI4J_DEFAULT_ROOT_PATH;
    private CompoundDocsProperties compoundDocs = new CompoundDocsProperties();
    private OasProperties oas = new OasProperties();
}
