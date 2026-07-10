package pro.api4.jsonapi4j.config;

/**
 * Configuration for the built-in introspection ("meta") API, bound from {@code jsonapi4j.meta.*}.
 * <p>
 * Disabled by default: the meta API exposes the application's internal structure, so it is opt-in.
 */
public interface MetaProperties {

    String ENABLED_PROPERTY = "enabled";

    String DEFAULT_ENABLED = "false";

    /**
     * @return {@code true} if the meta API ({@code /{rootPath}/state/this} and friends) is enabled. Default {@code false}.
     */
    default boolean enabled() {
        return false;
    }

}
