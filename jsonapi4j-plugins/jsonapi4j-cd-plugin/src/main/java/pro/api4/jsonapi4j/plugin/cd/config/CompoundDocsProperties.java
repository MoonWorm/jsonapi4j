package pro.api4.jsonapi4j.plugin.cd.config;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PropertiesValidationResult;
import pro.api4.jsonapi4j.config.PropertiesValidationResult.PropertiesValidationResultBuilder;

import java.net.URI;
import java.util.*;

public interface CompoundDocsProperties extends PluginProperties {

    String CD_PROPERTY = "cd";

    String ENABLED_PROPERTY = "enabled";
    String MAX_HOPS_PROPERTY = "maxHops";
    String MAX_INCLUDED_RESOURCES_PROPERTY = "maxIncludedResources";
    String ERROR_STRATEGY_PROPERTY = "errorStrategy";
    String MAPPING_PROPERTY = "mapping";
    /**
     * Reserved {@code mapping} key: its {@code url} is this app's own base URL, to fetch the resource types it serves
     * over HTTP from rather than in-process. A resource type can therefore not be named {@code default}.
     */
    String DEFAULT_MAPPING_KEY = "default";
    String DEFAULT_MAX_BATCH_SIZE_PROPERTY = "defaultMaxBatchSize";
    String PROPAGATION_PROPERTY = "propagation";
    String DEDUPLICATION_PROPERTY = "deduplication";
    String UNSUPPORTED_INCLUDES_PROPERTY = "unsupportedIncludes";
    String CREDENTIAL_HEADERS_PROPERTY = "credentialHeaders";
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
    String DEFAULT_DEDUPLICATION = "DATA_AND_INCLUDED";
    String DEFAULT_UNSUPPORTED_INCLUDES = "FAIL";
    /**
     * The headers carrying the client's identity: HTTP authentication, cookies, and those the framework's
     * {@code DefaultPrincipalResolver} reads by default.
     */
    String DEFAULT_CREDENTIAL_HEADERS = "Authorization,Cookie,Proxy-Authorization,X-Authenticated-User-Id,"
            + "X-Authenticated-User-Granted-Scopes,X-Authenticated-Client-Entitlements";
    String DEFAULT_HTTP_CONNECT_TIMEOUT_MS = "5000";
    String DEFAULT_HTTP_TOTAL_TIMEOUT_MS = "10000";
    /**
     * Derived from {@link DomainSettings#DEFAULT_MAX_BATCH_SIZE} so the resolver and the plugin share one default. Kept
     * a compile-time constant {@code String}, like the other defaults, so it can be used in annotations such as
     * Quarkus' {@code @WithDefault}.
     */
    String DEFAULT_MAX_BATCH_SIZE = "" + DomainSettings.DEFAULT_MAX_BATCH_SIZE;

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

    /**
     * @return the settings of each resource type, by resource type - see {@link Mapping}
     */
    default Map<String, ? extends Mapping> mapping() {
        return Collections.emptyMap();
    }

    /**
     * @return {@code mapping.default.url}, this app's own base URL - where resource types it serves are fetched from
     * over HTTP, see {@link #transportOf(String)} - if set
     */
    default Optional<String> defaultMapping() {
        return Optional.ofNullable(mapping())
                .map(mapping -> mapping.get(DEFAULT_MAPPING_KEY))
                .map(Mapping::url);
    }

    /**
     * @return the settings of each resource type mapped to a {@code url} - one served by another service - by
     * resource type
     */
    default Map<String, DomainSettings> domainSettings() {
        if (mapping() == null) {
            return Collections.emptyMap();
        }
        Map<String, DomainSettings> domainSettings = new HashMap<>();
        mapping().forEach((resourceType, mapping) -> {
            if (!DEFAULT_MAPPING_KEY.equals(resourceType) && mapping != null && mapping.url() != null) {
                domainSettings.put(resourceType, DomainSettings.overHttp(
                        URI.create(mapping.url()),
                        maxBatchSize(resourceType),
                        mapping.propagateCredentials()
                ));
            }
        });
        return Collections.unmodifiableMap(domainSettings);
    }

    /**
     * @return how resources of {@code resourceType}, one this app serves itself, are fetched: the {@code transport} of
     * its mapping, otherwise the one of {@code mapping.default}, otherwise {@link Transport#IN_PROCESS}
     */
    default Transport transportOf(String resourceType) {
        return transportOf(mappingOf(resourceType))
                .or(() -> transportOf(mappingOf(DEFAULT_MAPPING_KEY)))
                .orElse(Transport.IN_PROCESS);
    }

    private Mapping mappingOf(String resourceType) {
        return mapping() == null ? null : mapping().get(resourceType);
    }

    private static Optional<Transport> transportOf(Mapping mapping) {
        return Optional.ofNullable(mapping).map(Mapping::transport);
    }

    /**
     * @return the {@code maxBatchSize} of the mapping of {@code resourceType}, or {@link #defaultMaxBatchSize()}
     */
    default int maxBatchSize(String resourceType) {
        return Optional.ofNullable(mapping())
                .map(mapping -> mapping.get(resourceType))
                .map(Mapping::maxBatchSize)
                .orElse(defaultMaxBatchSize());
    }

    default int defaultMaxBatchSize() {
        return Integer.parseInt(DEFAULT_MAX_BATCH_SIZE);
    }

    default List<Propagation> propagation() {
        return parsePropagationString(DEFAULT_PROPAGATION);
    }

    default List<Propagation> parsePropagationString(String propagationString) {
        return parseCommaSeparated(propagationString)
                .stream()
                .map(Propagation::valueOf)
                .toList();
    }

    default Deduplication deduplication() {
        return Deduplication.valueOf(DEFAULT_DEDUPLICATION);
    }

    default UnsupportedIncludeStrategy unsupportedIncludes() {
        return UnsupportedIncludeStrategy.valueOf(DEFAULT_UNSUPPORTED_INCLUDES);
    }

    /**
     * @return the headers carrying the client's identity, sent only where {@link Mapping#propagateCredentials()}
     * allows - and always to resource types served by this app itself
     */
    default List<String> credentialHeaders() {
        return parseCommaSeparated(DEFAULT_CREDENTIAL_HEADERS);
    }

    static List<String> parseCommaSeparated(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .toList();
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
                .requireNotNull(propertyPath(PROPAGATION_PROPERTY), propagation())
                .requireNotNull(propertyPath(DEDUPLICATION_PROPERTY), deduplication())
                .requireNotNull(propertyPath(UNSUPPORTED_INCLUDES_PROPERTY), unsupportedIncludes())
                .requireNotNull(propertyPath(CREDENTIAL_HEADERS_PROPERTY), credentialHeaders());
        validateMapping(builder);
        validateCache(builder);
        validateTimeoutsBudget(builder);
        return builder.build();
    }

    /**
     * A resource type mapped to a {@code url} is fetched over HTTP from another service, so the URL has to be absolute -
     * a relative one has nothing to resolve against once the include leaves this app - and a {@code transport} of its
     * own is meaningless. A type without one is served by this app itself: fetched over HTTP only from
     * {@code mapping.default.url}, which has to be set then, and always with the client's credentials, so
     * {@code propagateCredentials} is meaningless there.
     */
    private void validateMapping(PropertiesValidationResultBuilder builder) {
        if (mapping() == null) {
            builder.requireNotNull(propertyPath(MAPPING_PROPERTY), null);
            return;
        }
        mapping().forEach((resourceType, mapping) -> {
            if (StringUtils.isBlank(resourceType)) {
                builder.addPropertyError(propertyPath(MAPPING_PROPERTY), "resource type must not be blank");
                return;
            }
            String mappingPath = propertyPath(MAPPING_PROPERTY, resourceType);
            if (mapping == null || mapping.url() == null && mapping.maxBatchSize() == null && mapping.transport() == null) {
                builder.addPropertyError(mappingPath, String.format(
                        "must set '%s', '%s' or '%s'",
                        Mapping.URL_PROPERTY, Mapping.MAX_BATCH_SIZE_PROPERTY, Mapping.TRANSPORT_PROPERTY
                ));
                return;
            }
            if (DEFAULT_MAPPING_KEY.equals(resourceType)) {
                validateDefaultMapping(builder, mapping);
                return;
            }
            if (mapping.url() != null) {
                builder.requireHttpUrl(propertyPath(MAPPING_PROPERTY, resourceType, Mapping.URL_PROPERTY), mapping.url());
            }
            if (mapping.maxBatchSize() != null) {
                builder.requirePositive(
                        propertyPath(MAPPING_PROPERTY, resourceType, Mapping.MAX_BATCH_SIZE_PROPERTY),
                        mapping.maxBatchSize()
                );
            }
            validateTransport(builder, resourceType, mapping);
            if (mapping.url() == null && mapping.propagateCredentials()) {
                builder.addPropertyError(
                        propertyPath(MAPPING_PROPERTY, resourceType, Mapping.PROPAGATE_CREDENTIALS_PROPERTY),
                        String.format("applies only to a resource type mapped to a '%s' - one served by this app itself "
                                + "always gets the client's credentials", Mapping.URL_PROPERTY)
                );
            }
        });
    }

    private void validateTransport(PropertiesValidationResultBuilder builder, String resourceType, Mapping mapping) {
        String transportPath = propertyPath(MAPPING_PROPERTY, resourceType, Mapping.TRANSPORT_PROPERTY);
        if (mapping.url() != null && mapping.transport() == Transport.IN_PROCESS) {
            builder.addPropertyError(transportPath, String.format(
                    "a resource type mapped to a '%s' is served by another service, so it is fetched over HTTP",
                    Mapping.URL_PROPERTY
            ));
        }
        if (mapping.url() == null && mapping.transport() == Transport.HTTP && defaultMapping().isEmpty()) {
            builder.addPropertyError(transportPath, String.format(
                    "needs '%s' - this app's own address - to fetch the resource type over HTTP from",
                    propertyPath(MAPPING_PROPERTY, DEFAULT_MAPPING_KEY, Mapping.URL_PROPERTY)
            ));
        }
    }

    /**
     * The reserved {@code default} entry says where this app is reached, and how the resource types it serves are
     * fetched by default. Batch sizes of those types come from their own entries or {@code defaultMaxBatchSize}, and
     * they always get the client's credentials.
     */
    private void validateDefaultMapping(PropertiesValidationResultBuilder builder, Mapping mapping) {
        if (mapping.url() != null) {
            builder.requireHttpUrl(
                    propertyPath(MAPPING_PROPERTY, DEFAULT_MAPPING_KEY, Mapping.URL_PROPERTY),
                    mapping.url()
            );
        }
        if (mapping.transport() == Transport.HTTP && mapping.url() == null) {
            builder.addPropertyError(
                    propertyPath(MAPPING_PROPERTY, DEFAULT_MAPPING_KEY, Mapping.TRANSPORT_PROPERTY),
                    String.format("needs '%s' - this app's own address - to fetch over HTTP from", Mapping.URL_PROPERTY)
            );
        }
        if (mapping.maxBatchSize() != null || mapping.propagateCredentials()) {
            builder.addPropertyError(
                    propertyPath(MAPPING_PROPERTY, DEFAULT_MAPPING_KEY),
                    String.format(
                            "the reserved '%s' entry takes only '%s' and '%s'",
                            DEFAULT_MAPPING_KEY, Mapping.URL_PROPERTY, Mapping.TRANSPORT_PROPERTY
                    )
            );
        }
    }

    /**
     * An absent {@code cache} section is fine - the plugin falls back to the cache defaults.
     */
    private void validateCache(PropertiesValidationResultBuilder builder) {
        if (!cacheEnabled()) {
            return;
        }
        builder.requirePositive(propertyPath(CACHE_PROPERTY, Cache.MAX_SIZE_PROPERTY), cacheMaxSize());
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

    /**
     * Whether resolved includes are cached. The {@code cache} section is optional, so an absent one means the
     * documented default applies - read this rather than {@link #cache()} to get that fallback for free.
     */
    default boolean cacheEnabled() {
        return cache() == null ? Boolean.parseBoolean(Cache.DEFAULT_CACHE_ENABLED) : cache().enabled();
    }

    /**
     * How many resolved resources the cache holds, falling back to the documented default when the {@code cache}
     * section is absent.
     */
    default int cacheMaxSize() {
        return cache() == null ? Integer.parseInt(Cache.DEFAULT_CACHE_MAX_SIZE) : cache().maxSize();
    }

    /**
     * The settings of one resource type under {@code mapping}. With a {@code url} the type is served by another service
     * and fetched from there; without one it is served by this app itself, fetched per its {@code transport}.
     */
    interface Mapping {

        String URL_PROPERTY = "url";
        String MAX_BATCH_SIZE_PROPERTY = "maxBatchSize";
        String PROPAGATE_CREDENTIALS_PROPERTY = "propagateCredentials";
        String TRANSPORT_PROPERTY = "transport";

        String DEFAULT_PROPAGATE_CREDENTIALS = "false";

        /**
         * @return the base URL of the service serving the resource type, or {@code null} when this app serves it
         */
        String url();

        /**
         * @return the maximum number of resource IDs per {@code filter[id]=...} fetch, or {@code null} for
         * {@code defaultMaxBatchSize}
         */
        Integer maxBatchSize();

        /**
         * @return whether the client's credentials - see {@code credentialHeaders} - are sent to the service at
         * {@link #url()}. Off by default, so a service doesn't get the client's identity unless trusted with it
         */
        boolean propagateCredentials();

        /**
         * @return how the resource type is fetched when this app serves it, or {@code null} to follow
         * {@code mapping.default} - see {@link CompoundDocsProperties#transportOf(String)}. On {@code mapping.default}
         * itself: how every resource type the app serves is fetched unless it says otherwise
         */
        Transport transport();

    }

    interface Cache {

        String ENABLED_PROPERTY = "enabled";
        String MAX_SIZE_PROPERTY = "maxSize";

        String DEFAULT_CACHE_ENABLED = "true";
        String DEFAULT_CACHE_MAX_SIZE = "1000";

        boolean enabled();

        int maxSize();

    }

}
