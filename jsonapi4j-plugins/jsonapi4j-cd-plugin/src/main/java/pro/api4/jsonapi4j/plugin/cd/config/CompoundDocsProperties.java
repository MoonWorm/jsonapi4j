package pro.api4.jsonapi4j.plugin.cd.config;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PropertiesValidationResult;
import pro.api4.jsonapi4j.config.PropertiesValidationResult.PropertiesValidationResultBuilder;

import java.util.*;

public interface CompoundDocsProperties extends PluginProperties {

    String CD_PROPERTY = "cd";

    String ENABLED_PROPERTY = "enabled";
    String MAX_HOPS_PROPERTY = "maxHops";
    String MAX_INCLUDED_RESOURCES_PROPERTY = "maxIncludedResources";
    String ERROR_STRATEGY_PROPERTY = "errorStrategy";
    String MAPPING_PROPERTY = "mapping";
    String BATCH_SIZE_MAPPING_PROPERTY = "batchSizeMapping";
    String DEFAULT_MAX_BATCH_SIZE_PROPERTY = "defaultMaxBatchSize";
    String PROPAGATION_PROPERTY = "propagation";
    String DEDUPLICATE_RESOURCES_PROPERTY = "deduplicateResources";
    String HTTP_CONNECT_TIMEOUT_MS_PROPERTY = "httpConnectTimeoutMs";
    String HTTP_TOTAL_TIMEOUT_MS_PROPERTY = "httpTotalTimeoutMs";
    String CACHE_PROPERTY = "cache";

    @Override
    default String section() {
        return CD_PROPERTY;
    }

    String DEFAULT_ENABLED = "false";
    String DEFAULT_MAX_HOPS = "2";
    String DEFAULT_MAX_INCLUDED_RESOURCES = "100";
    String DEFAULT_ERROR_STRATEGY = "IGNORE";
    String DEFAULT_PROPAGATION = "FIELDS,CUSTOM_QUERY_PARAMS,HEADERS";
    String DEFAULT_DEDUPLICATE_RESOURCES = "true";
    String DEFAULT_HTTP_CONNECT_TIMEOUT_MS = "5000";
    String DEFAULT_HTTP_TOTAL_TIMEOUT_MS = "10000";
    String DEFAULT_MAX_BATCH_SIZE = "20";

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

    default Map<String, Integer> batchSizeMapping() {
        return Collections.emptyMap();
    }

    default int defaultMaxBatchSize() {
        return Integer.parseInt(DEFAULT_MAX_BATCH_SIZE);
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

    @Override
    default PropertiesValidationResult validate() {
        if (!enabled()) {
            return PropertiesValidationResult.empty();
        }
        PropertiesValidationResultBuilder builder = PropertiesValidationResult.builder()
                .requirePositive(propertyPath(MAX_HOPS_PROPERTY), maxHops())
                .requirePositive(propertyPath(MAX_INCLUDED_RESOURCES_PROPERTY), maxIncludedResources())
                .requirePositive(propertyPath(DEFAULT_MAX_BATCH_SIZE_PROPERTY), defaultMaxBatchSize())
                .requirePositive(propertyPath(HTTP_CONNECT_TIMEOUT_MS_PROPERTY), httpConnectTimeoutMs())
                .requirePositive(propertyPath(HTTP_TOTAL_TIMEOUT_MS_PROPERTY), httpTotalTimeoutMs())
                .requireNotNull(propertyPath(ERROR_STRATEGY_PROPERTY), errorStrategy())
                .requireNotNull(propertyPath(PROPAGATION_PROPERTY), propagation());
        validateMapping(builder);
        validateBatchSizeMapping(builder);
        validateCache(builder);
        validateTimeoutsBudget(builder);
        return builder.build();
    }

    /**
     * Every mapped resource type is fetched over HTTP from another service, so its base URL has to be absolute -
     * a relative one has nothing to resolve against once the include leaves this app.
     */
    private void validateMapping(PropertiesValidationResultBuilder builder) {
        if (mapping() == null) {
            builder.requireNotNull(propertyPath(MAPPING_PROPERTY), null);
            return;
        }
        mapping().forEach((resourceType, baseUrl) -> {
            if (StringUtils.isBlank(resourceType)) {
                builder.addPropertyError(propertyPath(MAPPING_PROPERTY), "resource type must not be blank");
                return;
            }
            builder.requireHttpUrl(propertyPath(MAPPING_PROPERTY, resourceType), baseUrl);
        });
    }

    private void validateBatchSizeMapping(PropertiesValidationResultBuilder builder) {
        if (batchSizeMapping() == null) {
            builder.requireNotNull(propertyPath(BATCH_SIZE_MAPPING_PROPERTY), null);
            return;
        }
        batchSizeMapping().forEach((resourceType, batchSize) -> {
            if (StringUtils.isBlank(resourceType)) {
                builder.addPropertyError(propertyPath(BATCH_SIZE_MAPPING_PROPERTY), "resource type must not be blank");
                return;
            }
            builder.requirePositive(propertyPath(BATCH_SIZE_MAPPING_PROPERTY, resourceType), batchSize);
        });
    }

    /**
     * An absent {@code cache} section is fine - the plugin falls back to the cache defaults.
     */
    private void validateCache(PropertiesValidationResultBuilder builder) {
        if (cache() == null || !cache().enabled()) {
            return;
        }
        builder.requirePositive(propertyPath(CACHE_PROPERTY, Cache.MAX_SIZE_PROPERTY), cache().maxSize());
    }

    private void validateTimeoutsBudget(PropertiesValidationResultBuilder builder) {
        if (httpConnectTimeoutMs() > 0 && httpTotalTimeoutMs() > 0 && httpTotalTimeoutMs() < httpConnectTimeoutMs()) {
            builder.addCrossPropertiesError(String.format(
                    "'%s' (%d) must not be less than '%s' (%d): the total budget of an include call has to cover " +
                            "connecting to the remote service",
                    propertyPath(HTTP_TOTAL_TIMEOUT_MS_PROPERTY), httpTotalTimeoutMs(),
                    propertyPath(HTTP_CONNECT_TIMEOUT_MS_PROPERTY), httpConnectTimeoutMs()
            ));
        }
    }

    Cache cache();

    interface Cache {

        String ENABLED_PROPERTY = "enabled";
        String MAX_SIZE_PROPERTY = "maxSize";

        String DEFAULT_CACHE_ENABLED = "true";
        String DEFAULT_CACHE_MAX_SIZE = "1000";

        boolean enabled();

        int maxSize();

    }

}
