package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties;

import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;

@Singleton
@ConfigMapping(prefix = "jsonapi4j.sf", namingStrategy = VERBATIM)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface QuarkusJsonApi4jSfProperties {

    /**
     * Enable/disable the JsonApi4j Sparse Fieldsets plugin.
     * Enabled by default.
     * Example: `true`, `false`.
     */
    @WithDefault(SfProperties.DEFAULT_SF_ENABLED)
    boolean enabled();

}
