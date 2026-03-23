package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.plugin.sf.config.DefaultSfProperties;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties;
import pro.api4.jsonapi4j.plugin.sf.config.SfProperties.RequestedFieldsDontExistMode;

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

    /**
     * Defines strategies of how to behave in situations when some fields were requested but none exists
     * Uses 'SPARSE_ALL_FIELDS' mode by default.
     * Two options available: 'RETURN_ALL_FIELDS' / 'SPARSE_ALL_FIELDS'
     */
    @WithDefault(SfProperties.DEFAULT_REQUESTED_FIELDS_DONT_EXIST_MODE)
    RequestedFieldsDontExistMode requestedFieldsDontExistMode();

    default SfProperties toJsonapi4jSfProperties() {
        DefaultSfProperties sfProperties = new DefaultSfProperties();
        sfProperties.setEnabled(enabled());
        sfProperties.setRequestedFieldsDontExistMode(requestedFieldsDontExistMode());
        return sfProperties;
    }

}
