package pro.api4.jsonapi4j.plugin.cd.config;

import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;

import java.util.*;

public interface CompoundDocsProperties {

    String CD_PROPERTY_NAME = "cd";

    String CD_ENABLED_DEFAULT_VALUE = "false";
    String CD_MAX_HOPS_DEFAULT_VALUE = "2";
    String CD_ERROR_STRATEGY_DEFAULT_VALUE = "IGNORE";
    String CD_PROPAGATION_DEFAULT_VALUE = "FIELDS,CUSTOM_QUERY_PARAMS,HEADERS";

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
        return Collections.emptyMap();
    }

    default List<Propagation> propagation() {
        return parsePropagationString(CD_PROPAGATION_DEFAULT_VALUE);
    }

    default List<Propagation> parsePropagationString(String propagationString) {
        return Arrays.stream(propagationString.split(","))
                .map(String::trim)
                .map(Propagation::valueOf)
                .toList();
    }

}
