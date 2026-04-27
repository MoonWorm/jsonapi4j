package pro.api4.jsonapi4j.config;

import pro.api4.jsonapi4j.operation.validation.ValidationProperties;

public interface JsonApi4jProperties {

    String DEFAULT_ROOT_PATH = "/jsonapi";

    default String rootPath() {
        return DEFAULT_ROOT_PATH;
    }

    ValidationProperties validation();

}
