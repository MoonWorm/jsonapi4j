package pro.api4.jsonapi4j.plugin.sf.config;

public interface SfProperties {

    String DEFAULT_SF_ENABLED = "true";

    default boolean enabled() {
        return Boolean.parseBoolean(DEFAULT_SF_ENABLED);
    }

}
