package pro.api4.jsonapi4j.rest.quarkus.runtime.cd;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.plugin.cd.config.CompoundDocsProperties;
import pro.api4.jsonapi4j.plugin.cd.config.DefaultCompoundDocsProperties;

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
     * Error strategy.
     */
    @WithDefault(CD_ERROR_STRATEGY_DEFAULT_VALUE)
    ErrorStrategy errorStrategy();

    /**
     * Per-resource mapping for downstream URLs.
     */
    Map<String, String> mapping();

    default CompoundDocsProperties toCdProperties() {
        DefaultCompoundDocsProperties cdProperties = new DefaultCompoundDocsProperties();
        cdProperties.setEnabled(enabled());
        cdProperties.setMaxHops(maxHops());
        cdProperties.setErrorStrategy(errorStrategy());
        cdProperties.setMapping(mapping());
        return cdProperties;
    }
}
