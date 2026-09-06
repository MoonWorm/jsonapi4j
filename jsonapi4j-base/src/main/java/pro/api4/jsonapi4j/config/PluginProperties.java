package pro.api4.jsonapi4j.config;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

    /**
     * Validates the bound configuration is usable: mandatory values are set, numbers are within range, URLs parse,
     * and properties that depend on each other agree. Implemented once per plugin on the {@code *Properties}
     * interface, so every host stack (Spring Boot, Quarkus, Servlet) validates identically.
     * <p>
     * Called at startup by {@code JsonApi4jBuilder}, which fails the build when any registered plugin reports errors.
     * An implementation returns every error it finds rather than the first one, and returns
     * {@link PluginPropertiesValidationResult#empty()} when the plugin is disabled - dormant config never breaks a
     * boot.
     *
     * @return the collected errors, never {@code null}
     */
    default PluginPropertiesValidationResult validate() {
        return PluginPropertiesValidationResult.empty();
    }

    /**
     * Builds the fully-qualified key of one of this section's properties, the way a user writes it in their config,
     * e.g. {@code propertyPath("info", "title")} on the OAS plugin yields {@code jsonapi4j.oas.info.title}. Use it
     * for every path reported to {@link PluginPropertiesValidationResult}, so an error points at a key that can be
     * searched for as-is.
     *
     * @param path the property names, from the section root down, indexes included (e.g. {@code "servers[0]"})
     * @return the config key prefixed with {@code jsonapi4j.<section>}
     */
    default String propertyPath(String... path) {
        return Stream.concat(
                Stream.of(JsonApi4jProperties.CONFIG_PREFIX, section()),
                Arrays.stream(path)
        ).collect(Collectors.joining("."));
    }

}
