package pro.api4.jsonapi4j.plugin.ac.config;

import pro.api4.jsonapi4j.config.PluginProperties;

public interface AcProperties extends PluginProperties {

    String AC_PROPERTY = "ac";
    String ENABLED_PROPERTY = "enabled";
    String FAIL_ON_MISCONFIGURATION_PROPERTY = "failOnMisconfiguration";

    String DEFAULT_ENABLED = "true";
    String DEFAULT_FAIL_ON_MISCONFIGURATION = "false";

    @Override
    default String section() {
        return AC_PROPERTY;
    }

    default boolean enabled() {
        return Boolean.parseBoolean(DEFAULT_ENABLED);
    }

    /**
     * Whether access control that is declared but cannot take effect is rejected rather than reported.
     *
     * <p>Covers requirements that cannot take effect: on a primitive or static field, on a collection
     * element, on a shadowed field name, or one that declares nothing. Diagnostics about the caller — such
     * as a principal arriving with no entitlements — are not covered and remain warnings.
     *
     * <p>Not startup validation. Requirements on a resource, operation or relationship class are read when
     * the plugin registers, but an attributes class is only known once a response carries one, so most are
     * raised on the first request that touches the resource.
     *
     * @return {@code true} to reject rather than report
     */
    default boolean failOnMisconfiguration() {
        return Boolean.parseBoolean(DEFAULT_FAIL_ON_MISCONFIGURATION);
    }

}
