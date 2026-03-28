package pro.api4.jsonapi4j.rest.quarkus.runtime.cd;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;

import java.util.List;
import java.util.Map;

import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;
import static pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties.*;

@Singleton
@ConfigMapping(prefix = "jsonapi4j.cd", namingStrategy = VERBATIM)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface QuarkusJsonApi4jCompoundDocsProperties {

    /**
     * Enables compound documents filter.
     */
    @WithDefault(CD_ENABLED_DEFAULT_VALUE)
    boolean enabled();

    /**
     * Maximum include traversal depth.
     */
    @WithDefault(CD_MAX_HOPS_DEFAULT_VALUE)
    int maxHops();

    /**
     * Maximum amount of included resources.
     */
    @WithDefault(CD_MAX_INCLUDED_RESOURCES)
    int maxIncludedResources();

    /**
     * Error strategy.
     */
    @WithDefault(CD_ERROR_STRATEGY_DEFAULT_VALUE)
    ErrorStrategy errorStrategy();

    /**
     * Per-resource mapping for downstream URLs.
     */
    Map<String, String> mapping();

    /**
     * Defines which JsonApiRequest parts to propagate during Compound Docs resolution loop.
     */
    @WithDefault(CD_PROPAGATION_DEFAULT_VALUE)
    List<Propagation> propagation();

    /**
     * Defines if Compound Docs plugin should deduplicate resources in 'included' section (by 'type' / 'id')
     */
    @WithDefault(CD_DEDUPLICATE_RESOURCES_DEFAULT_VALUE)
    boolean deduplicateResources();

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
    @WithDefault(CD_HTTP_CONNECT_TIMEOUT_MS_DEFAULT_VALUE)
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
    @WithDefault(CD_HTTP_TOTAL_TIMEOUT_MS_DEFAULT_VALUE)
    long httpTotalTimeoutMs();

    default CompoundDocsProperties toCdProperties() {
        DefaultCompoundDocsProperties cdProperties = new DefaultCompoundDocsProperties();
        cdProperties.setEnabled(enabled());
        cdProperties.setMaxHops(maxHops());
        cdProperties.setMaxIncludedResources(maxIncludedResources());
        cdProperties.setErrorStrategy(errorStrategy());
        cdProperties.setMapping(mapping());
        cdProperties.setPropagation(propagation());
        cdProperties.setDeduplicateResources(deduplicateResources());
        cdProperties.setHttpConnectTimeoutMs(httpConnectTimeoutMs());
        cdProperties.setHttpTotalTimeoutMs(httpTotalTimeoutMs());
        return cdProperties;
    }
}
