package pro.api4.jsonapi4j.config;

/**
 * Configuration exposed by a jsonapi4j plugin, self-describing via the {@code jsonapi4j.*} subtree it occupies.
 * <p>
 * Implemented by each plugin's {@code *Properties} type (e.g. {@code CompoundDocsProperties}), it lets the plugin
 * contribute its effective, non-secret configuration to composed views (such as the built-in {@code config} meta
 * resource) as a strict type keyed by its {@link #section()} prefix, without any flat-key reconstruction.
 */
public interface PluginProperties {

    /**
     * @return the config subtree prefix under {@code jsonapi4j.*} this configuration occupies, e.g. {@code "cd"}.
     */
    String section();

}
