package pro.api4.jsonapi4j.rest.quarkus.runtime.ac;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.plugin.ac.config.AcProperties;
import pro.api4.jsonapi4j.plugin.ac.config.AnonymizationReportLevel;
import pro.api4.jsonapi4j.plugin.ac.config.DefaultAcProperties;

import static io.smallrye.config.ConfigMapping.NamingStrategy.VERBATIM;

@Singleton
@ConfigMapping(prefix = "jsonapi4j.ac", namingStrategy = VERBATIM)
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
public interface QuarkusJsonApi4jAcProperties {

    /**
     * Enable/disable the JsonApi4j Access Control plugin.
     * Enabled by default.
     * Example: `true`, `false`.
     */
    @WithDefault(AcProperties.DEFAULT_ENABLED)
    boolean enabled();

    /**
     * Reject access control that is declared but cannot take effect, instead of only reporting it.
     * Disabled by default.
     * Example: `true`, `false`.
     */
    @WithDefault(AcProperties.DEFAULT_FAIL_ON_MISCONFIGURATION)
    boolean failOnMisconfiguration();

    /**
     * How much a response says about what access control hid from the caller.
     * Nothing by default, because saying so confirms that something is there.
     * Example: `NONE`, `INDICATOR`, `FIELDS`, `FIELDS_AND_REASONS`.
     */
    @WithDefault(AcProperties.DEFAULT_ANONYMIZATION_REPORT)
    AnonymizationReportLevel anonymizationReport();

    default AcProperties toJsonapi4jAcProperties() {
        DefaultAcProperties acProperties = new DefaultAcProperties();
        acProperties.setEnabled(enabled());
        acProperties.setFailOnMisconfiguration(failOnMisconfiguration());
        acProperties.setAnonymizationReport(anonymizationReport());
        return acProperties;
    }

}
