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
     * Whether access control that is declared but cannot take effect should be rejected instead of merely
     * reported.
     *
     * <p>Off by default, because turning a warning into a failure would break an application that has been
     * running happily with a rule that quietly does nothing. Turning it on is worth it in tests and CI,
     * where a rule silently not applying is exactly what you want to hear about loudly.
     *
     * <p>This covers only what is decidable from a class — a requirement on a primitive or static field, on
     * a collection element, or one that asks for nothing. It deliberately does not cover diagnostics about
     * the caller, such as a principal arriving with no entitlements: that is a token or deployment problem,
     * and those requests already fail closed.
     *
     * <p>Note that it is not startup validation. Requirements on a resource, operation or relationship
     * class are read when the plugin registers, but an attributes class is only known once a response
     * carries one — so most of these are raised on the first request that touches the resource.
     *
     * @return {@code true} to reject rather than report
     */
    default boolean failOnMisconfiguration() {
        return Boolean.parseBoolean(DEFAULT_FAIL_ON_MISCONFIGURATION);
    }

}
