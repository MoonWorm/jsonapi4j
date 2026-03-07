package pro.api4.jsonapi4j.config;

import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig.ErrorStrategy;

import java.util.Collections;
import java.util.Map;

public interface CompoundDocsProperties {

    String JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE = "false";
    String JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE = "2";
    String JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE = "IGNORE";
    Map<String, String> JSONAPI4J_COMPOUND_DOCS_MAPPING_DEFAULT_VALUE = Collections.emptyMap();

    default boolean enabled() {
        return Boolean.parseBoolean(JSONAPI4J_COMPOUND_DOCS_ENABLED_DEFAULT_VALUE);
    }

    default int maxHops() {
        return Integer.parseInt(JSONAPI4J_COMPOUND_DOCS_MAX_HOPS_DEFAULT_VALUE);
    }

    default ErrorStrategy errorStrategy() {
        return ErrorStrategy.valueOf(JSONAPI4J_COMPOUND_DOCS_ERROR_STRATEGY_DEFAULT_VALUE);
    }

    default Map<String, String> mapping() {
        return JSONAPI4J_COMPOUND_DOCS_MAPPING_DEFAULT_VALUE;
    }

}
