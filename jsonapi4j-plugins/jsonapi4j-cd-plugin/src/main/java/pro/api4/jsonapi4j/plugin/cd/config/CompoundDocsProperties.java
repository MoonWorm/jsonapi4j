package pro.api4.jsonapi4j.plugin.cd.config;

import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;

import java.util.HashMap;
import java.util.Map;

public interface CompoundDocsProperties {

    String CD_PROPERTY_NAME = "cd";

    String CD_ENABLED_DEFAULT_VALUE = "false";
    String CD_MAX_HOPS_DEFAULT_VALUE = "2";
    String CD_ERROR_STRATEGY_DEFAULT_VALUE = "IGNORE";
    Map<String, String> CD_MAPPING_DEFAULT_VALUE = new HashMap<>();

    default boolean enabled() {
        return Boolean.parseBoolean(CD_ENABLED_DEFAULT_VALUE);
    }

    default int maxHops() {
        return Integer.parseInt(CD_MAX_HOPS_DEFAULT_VALUE);
    }

    default ErrorStrategy errorStrategy() {
        return ErrorStrategy.valueOf(CD_ERROR_STRATEGY_DEFAULT_VALUE);
    }

    default Map<String, String> mapping() {
        return CD_MAPPING_DEFAULT_VALUE;
    }

}
