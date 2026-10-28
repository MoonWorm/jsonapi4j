package pro.api4.jsonapi4j.config;

/**
 * Configuration for the JSON:API entry point served at {@code GET {rootPath}}, bound from
 * {@code jsonapi4j.entryPoint.*}.
 * <p>
 * Enabled by default: the entry-point document only links to the resource collections the API already exposes.
 */
public interface EntryPointProperties {

    String ENABLED_PROPERTY = "enabled";

    String DEFAULT_ENABLED = "true";

    /**
     * @return {@code true} if {@code GET {rootPath}} answers with the entry-point document. Default {@code true}.
     */
    default boolean enabled() {
        return true;
    }

}
