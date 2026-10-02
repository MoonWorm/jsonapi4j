package pro.api4.jsonapi4j.rest.quarkus.runtime.cd;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.config.Transport;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;
import static pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties.*;
import static pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties.Cache.DEFAULT_CACHE_ENABLED;
import static pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties.Cache.DEFAULT_CACHE_MAX_SIZE;

@Singleton
@ConfigMapping(prefix = "jsonapi4j.cd", namingStrategy = VERBATIM)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface QuarkusJsonApi4jCompoundDocsProperties {

    /**
     * Enables compound documents filter.
     */
    @WithDefault(DEFAULT_ENABLED)
    boolean enabled();

    /**
     * Maximum include traversal depth.
     */
    @WithDefault(DEFAULT_MAX_HOPS)
    int maxHops();

    /**
     * Maximum amount of included resources.
     */
    @WithDefault(DEFAULT_MAX_INCLUDED_RESOURCES)
    int maxIncludedResources();

    /**
     * Error strategy.
     */
    @WithDefault(DEFAULT_ERROR_STRATEGY)
    ErrorStrategy errorStrategy();

    /**
     * Settings per resource type: 'url' of the service serving it (none when this app serves it), 'maxBatchSize',
     * 'propagateCredentials' and 'transport'. The reserved 'default' entry takes this app's own base URL as 'url', and
     * the 'transport' of the resource types it serves that set none.
     */
    Map<String, Mapping> mapping();

    /**
     * Fallback maximum number of resource IDs per downstream {@code filter[id]=...} batch when no
     * per-resource override is configured under {@link #mapping()}. The Compound Documents
     * Resolver splits larger ID sets into parallel chunks of this size.
     */
    @WithDefault(DEFAULT_MAX_BATCH_SIZE)
    int defaultMaxBatchSize();

    /**
     * Defines which JsonApiRequest parts to propagate during Compound Docs resolution loop.
     */
    @WithDefault(DEFAULT_PROPAGATION)
    List<Propagation> propagation();

    /**
     * How resource objects repeat across 'data' and 'included' (by 'type' / 'id').
     */
    @WithDefault(DEFAULT_DEDUPLICATION)
    Deduplication deduplication();

    /**
     * What a request with an unsupported 'include' path - deeper than 'maxHops', or naming a relationship its resource
     * type doesn't have - gets: FAIL answers 400 Bad Request, IGNORE resolves it only as far as supported and lists it
     * in 'meta.includedIncomplete'.
     */
    @WithDefault(DEFAULT_UNSUPPORTED_INCLUDES)
    UnsupportedIncludeStrategy unsupportedIncludes();

    /**
     * Headers carrying the client's identity, sent only to a mapping with 'propagateCredentials' - and always to
     * resource types served by this app itself.
     */
    @WithDefault(DEFAULT_CREDENTIAL_HEADERS)
    List<String> credentialHeaders();

    /**
     * Controls how long to wait when establishing TCP connection (in millisecond).
     * Covers:
     * <ul>
     *     <li>DNS resolution</li>
     *     <li>TCP handshake</li>
     * </ul>
     * <p>
     * Does not cover:
     * <ul>
     *     <li>waiting for response</li>
     *     <li>reading body</li>
     * </ul>
     */
    @WithDefault(DEFAULT_HTTP_CONNECT_TIMEOUT_MS)
    long httpConnectTimeoutMs();

    /**
     * Controls total request timeout (in millisecond).
     * Covers:
     * <ul>
     *     <li>connection</li>
     *     <li>sending request</li>
     *     <li>waiting for response</li>
     *     <li>reading response body</li>
     * </ul>
     */
    @WithDefault(DEFAULT_HTTP_TOTAL_TIMEOUT_MS)
    long httpTotalTimeoutMs();

    /**
     * Cache settings. Optional.
     */
    Optional<CacheConfig> cache();

    /**
     * Cache configuration under {@code jsonapi4j.cd.cache.*}.
     */
    interface CacheConfig {
        /**
         * Enables compound docs resource cache.
         */
        @WithDefault(DEFAULT_CACHE_ENABLED)
        boolean enabled();

        /**
         * Maximum number of entries in the in-memory cache.
         */
        @WithDefault(DEFAULT_CACHE_MAX_SIZE)
        int maxSize();
    }

    interface Mapping {

        /**
         * Base URL of the service serving the resource type - none when this app serves it.
         */
        Optional<String> url();

        /**
         * Maximum number of resource IDs per downstream {@code filter[id]=...} batch, overriding
         * 'defaultMaxBatchSize'.
         */
        Optional<Integer> maxBatchSize();

        /**
         * Whether the client's credentials are sent to the service at 'url'.
         */
        @WithDefault(CompoundDocsProperties.Mapping.DEFAULT_PROPAGATE_CREDENTIALS)
        boolean propagateCredentials();

        /**
         * How the resource type is fetched when this app serves it: IN_PROCESS, or HTTP from 'mapping.default.url'.
         */
        Optional<Transport> transport();
    }

    default CompoundDocsProperties toCdProperties() {
        DefaultCompoundDocsProperties cdProperties = new DefaultCompoundDocsProperties();
        cdProperties.setEnabled(enabled());
        cdProperties.setMaxHops(maxHops());
        cdProperties.setMaxIncludedResources(maxIncludedResources());
        cdProperties.setErrorStrategy(errorStrategy());
        cdProperties.setMapping(mapping().entrySet().stream().collect(Collectors.toMap(
                Map.Entry::getKey,
                e -> new DefaultCompoundDocsProperties.DefaultMapping(
                        e.getValue().url().orElse(null),
                        e.getValue().maxBatchSize().orElse(null),
                        e.getValue().propagateCredentials(),
                        e.getValue().transport().orElse(null)
                )
        )));
        cdProperties.setDefaultMaxBatchSize(defaultMaxBatchSize());
        cdProperties.setPropagation(propagation());
        cdProperties.setDeduplication(deduplication());
        cdProperties.setUnsupportedIncludes(unsupportedIncludes());
        cdProperties.setCredentialHeaders(credentialHeaders());
        cdProperties.setHttpConnectTimeoutMs(httpConnectTimeoutMs());
        cdProperties.setHttpTotalTimeoutMs(httpTotalTimeoutMs());
        cdProperties.setCache(cache().map(c -> {
            DefaultCompoundDocsProperties.DefaultCache dc = new DefaultCompoundDocsProperties.DefaultCache();
            dc.setEnabled(c.enabled());
            dc.setMaxSize(c.maxSize());
            return dc;
        }).orElse(null));
        return cdProperties;
    }
}
