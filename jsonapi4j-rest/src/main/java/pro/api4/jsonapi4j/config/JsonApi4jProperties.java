package pro.api4.jsonapi4j.config;

public interface JsonApi4jProperties {

    String JSONAPI4J_DEFAULT_ROOT_PATH = "/jsonapi";

    default String rootPath() {
        return JSONAPI4J_DEFAULT_ROOT_PATH;
    }

    default CompoundDocsProperties compoundDocs() {
        return new DefaultCompoundDocsProperties();
    }

}
