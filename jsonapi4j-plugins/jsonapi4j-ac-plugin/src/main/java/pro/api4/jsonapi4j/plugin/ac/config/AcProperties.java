package pro.api4.jsonapi4j.plugin.ac.config;

public interface AcProperties {

    String DEFAULT_AC_ENABLED = "true";

    default boolean enabled() {
        return Boolean.parseBoolean(DEFAULT_AC_ENABLED);
    }

}
