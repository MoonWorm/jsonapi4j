package pro.api4.jsonapi4j.plugin.cd.config;

import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;

import java.util.*;

public interface CompoundDocsProperties {

    String CD_PROPERTY_NAME = "cd";

    String DEFAULT_ENABLED = "false";
    String DEFAULT_MAX_HOPS = "2";
    String DEFAULT_MAX_INCLUDED_RESOURCES = "100";
    String DEFAULT_ERROR_STRATEGY = "IGNORE";
    String DEFAULT_PROPAGATION = "FIELDS,CUSTOM_QUERY_PARAMS,HEADERS";
    String DEFAULT_DEDUPLICATE_RESOURCES = "true";
    String DEFAULT_HTTP_CONNECT_TIMEOUT_MS = "5000";
    String DEFAULT_HTTP_TOTAL_TIMEOUT_MS = "10000";

    default boolean enabled() {
        return Boolean.parseBoolean(DEFAULT_ENABLED);
    }

    default int maxHops() {
        return Integer.parseInt(DEFAULT_MAX_HOPS);
    }

    default int maxIncludedResources() {
        return Integer.parseInt(DEFAULT_MAX_INCLUDED_RESOURCES);
    }

    default ErrorStrategy errorStrategy() {
        return ErrorStrategy.valueOf(DEFAULT_ERROR_STRATEGY);
    }

    default Map<String, String> mapping() {
        return Collections.emptyMap();
    }

    default List<Propagation> propagation() {
        return parsePropagationString(DEFAULT_PROPAGATION);
    }

    default List<Propagation> parsePropagationString(String propagationString) {
        return Arrays.stream(propagationString.split(","))
                .map(String::trim)
                .map(Propagation::valueOf)
                .toList();
    }

    default boolean deduplicateResources() {
        return Boolean.parseBoolean(DEFAULT_DEDUPLICATE_RESOURCES);
    }

    default long httpConnectTimeoutMs() {
        return Long.parseLong(DEFAULT_HTTP_CONNECT_TIMEOUT_MS);
    }

    default long httpTotalTimeoutMs() {
        return Long.parseLong(DEFAULT_HTTP_TOTAL_TIMEOUT_MS);
    }

    Cache cache();

    interface Cache {

        String DEFAULT_CACHE_ENABLED = "true";
        String DEFAULT_CACHE_MAX_SIZE = "1000";

        boolean enabled();

        int maxSize();

    }

}
