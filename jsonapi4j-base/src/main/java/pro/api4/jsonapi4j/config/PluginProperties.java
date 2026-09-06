package pro.api4.jsonapi4j.config;

/**
 * Configuration exposed by a jsonapi4j plugin, self-describing via the {@code jsonapi4j.*} subtree it occupies.
 * <p>
 * Implemented by each plugin's {@code *Properties} type (e.g. {@code CompoundDocsProperties}), it lets the plugin
 * contribute its effective, non-secret configuration to composed views (such as the built-in {@code config} meta
 * resource) as a strict type keyed by its {@link #section()} prefix, without any flat-key reconstruction.
 */
public interface PluginProperties extends ValidatableProperties {

    /**
     * @return the config subtree prefix under {@code jsonapi4j.*} this configuration occupies, e.g. {@code "cd"}.
     */
    String section();

    @Override
    default String propertyPathPrefix() {
        return JsonApi4jProperties.CONFIG_PREFIX + "." + section();
    }

    /**
     * Validates this plugin's configuration against the root configuration it runs under - the checks that no
     * section can make alone because they span two of them, such as a plugin's endpoint path having to agree with
     * {@code jsonapi4j.rootPath}.
     * <p>
     * Called at startup by {@code JsonApi4jBuilder}, which merges the result into the same report as
     * {@link #validate()}. Kept separate so {@link #validate()} stays self-contained: a section can always be
     * checked on its own, with no root configuration to hand.
     *
     * @param rootProperties the effective root configuration, never {@code null}
     * @return the collected errors, never {@code null}; {@link PropertiesValidationResult#empty()} when this plugin
     * has nothing to check against the root configuration
     */
    default PropertiesValidationResult validateAgainst(JsonApi4jProperties rootProperties) {
        return PropertiesValidationResult.empty();
    }

}
