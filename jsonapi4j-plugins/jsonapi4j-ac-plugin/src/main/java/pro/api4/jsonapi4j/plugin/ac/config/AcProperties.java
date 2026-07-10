package pro.api4.jsonapi4j.plugin.ac.config;

import pro.api4.jsonapi4j.config.PluginProperties;

public interface AcProperties extends PluginProperties {

    String AC_PROPERTY = "ac";
    String ENABLED_PROPERTY = "enabled";

    String DEFAULT_ENABLED = "true";

    @Override
    default String section() {
        return AC_PROPERTY;
    }

    default boolean enabled() {
        return Boolean.parseBoolean(DEFAULT_ENABLED);
    }

}
