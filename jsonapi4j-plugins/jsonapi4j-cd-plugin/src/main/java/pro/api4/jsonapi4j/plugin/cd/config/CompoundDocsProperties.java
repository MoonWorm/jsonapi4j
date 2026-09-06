package pro.api4.jsonapi4j.plugin.cd.config;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PluginPropertiesValidationResult;
import pro.api4.jsonapi4j.config.PluginPropertiesValidationResult.PluginPropertiesValidationResultBuilder;

import java.util.*;

public interface CompoundDocsProperties extends PluginProperties {

    String CD_PROPERTY = "cd";

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
    default PluginPropertiesValidationResult validate() {
        if (!enabled()) {
            return PluginPropertiesValidationResult.empty();
        }
        PluginPropertiesValidationResultBuilder builder = PluginPropertiesValidationResult.builder()
                .requirePositive(propertyPath("maxHops"), maxHops())
                .requirePositive(propertyPath("maxIncludedResources"), maxIncludedResources())
                .requirePositive(propertyPath("defaultMaxBatchSize"), defaultMaxBatchSize())
                .requirePositive(propertyPath("httpConnectTimeoutMs"), httpConnectTimeoutMs())
                .requirePositive(propertyPath("httpTotalTimeoutMs"), httpTotalTimeoutMs())
                .requireNotNull(propertyPath("errorStrategy"), errorStrategy())
                .requireNotNull(propertyPath("propagation"), propagation());
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
    private void validateMapping(PluginPropertiesValidationResultBuilder builder) {
        if (mapping() == null) {
            builder.requireNotNull(propertyPath("mapping"), null);
            return;
        }
        mapping().forEach((resourceType, baseUrl) -> {
            if (StringUtils.isBlank(resourceType)) {
                builder.addPropertyError(propertyPath("mapping"), "resource type must not be blank");
                return;
            }
            builder.requireHttpUrl(propertyPath("mapping", resourceType), baseUrl);
        });
    }

    private void validateBatchSizeMapping(PluginPropertiesValidationResultBuilder builder) {
        if (batchSizeMapping() == null) {
            builder.requireNotNull(propertyPath("batchSizeMapping"), null);
            return;
        }
        batchSizeMapping().forEach((resourceType, batchSize) -> {
            if (StringUtils.isBlank(resourceType)) {
                builder.addPropertyError(propertyPath("batchSizeMapping"), "resource type must not be blank");
                return;
            }
            builder.requirePositive(propertyPath("batchSizeMapping", resourceType), batchSize);
        });
    }

    /**
     * An absent {@code cache} section is fine - the plugin falls back to the cache defaults.
     */
    private void validateCache(PluginPropertiesValidationResultBuilder builder) {
        if (cache() == null || !cache().enabled()) {
            return;
        }
        builder.requirePositive(propertyPath("cache", "maxSize"), cache().maxSize());
    }

    private void validateTimeoutsBudget(PluginPropertiesValidationResultBuilder builder) {
        if (httpConnectTimeoutMs() > 0 && httpTotalTimeoutMs() > 0 && httpTotalTimeoutMs() < httpConnectTimeoutMs()) {
            builder.addCrossPropertiesError(String.format(
                    "'%s' (%d) must not be less than '%s' (%d): the total budget of an include call has to cover " +
                            "connecting to the remote service",
                    propertyPath("httpTotalTimeoutMs"), httpTotalTimeoutMs(),
                    propertyPath("httpConnectTimeoutMs"), httpConnectTimeoutMs()
            ));
        }
    }

    Cache cache();

    interface Cache {

        String DEFAULT_CACHE_ENABLED = "true";
        String DEFAULT_CACHE_MAX_SIZE = "1000";

        boolean enabled();

        int maxSize();

    }

}
