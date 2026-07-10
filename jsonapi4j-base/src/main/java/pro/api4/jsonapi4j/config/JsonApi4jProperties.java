package pro.api4.jsonapi4j.config;

import pro.api4.jsonapi4j.operation.validation.ValidationProperties;

public interface JsonApi4jProperties {

    String CONFIG_PREFIX = "jsonapi4j";

    String ROOT_PATH_PROPERTY = "rootPath";
    String META_PROPERTY = "meta";

    String DEFAULT_ROOT_PATH = "/jsonapi";

    default String rootPath() {
        return DEFAULT_ROOT_PATH;
    }

    ValidationProperties validation();

    MetaProperties meta();

}
