package pro.api4.jsonapi4j.config;

import java.util.Arrays;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A configuration section under {@code jsonapi4j.*} that can check itself at startup.
 * <p>
 * Implemented by the root configuration ({@link JsonApi4jProperties}, occupying {@code jsonapi4j}) and by every
 * plugin's configuration ({@link PluginProperties}, occupying {@code jsonapi4j.<section>}), so both are validated
 * on the same footing by {@code JsonApi4jBuilder} and report errors keyed by the very config keys a user writes.
 */
public interface ValidatableProperties {

    /**
     * The {@code jsonapi4j.*} subtree this configuration occupies, spelled the way a user spells it in their config
     * file, e.g. {@code jsonapi4j} for the root configuration or {@code jsonapi4j.oas} for the OpenAPI plugin.
     * <p>
     * Deliberately not a JavaBean-style getter: these objects are serialized into the built-in {@code config} meta
     * resource, and only {@code getX()} / {@code isX()} accessors are picked up there. Renaming this to
     * {@code getPropertyPathPrefix()} would leak the prefix into that document.
     *
     * @return the config key prefix, never {@code null}
     */
    String propertyPathPrefix();

    /**
     * Validates the bound configuration is usable: mandatory values are set, numbers are within range, URLs and
     * paths parse, and properties that depend on each other agree. Implemented once per section on the
     * {@code *Properties} interface, so every host stack (Spring Boot, Quarkus, Servlet) validates identically.
     * <p>
     * Called at startup by {@code JsonApi4jBuilder}, which fails the build when a section reports errors. An
     * implementation returns every error it finds rather than the first one, and a plugin returns
     * {@link PropertiesValidationResult#empty()} when it is disabled - dormant config never breaks a boot.
     *
     * @return the collected errors, never {@code null}
     */
    default PropertiesValidationResult validate() {
        return PropertiesValidationResult.empty();
    }

    /**
     * Builds the fully-qualified key of one of this section's properties, the way a user writes it in their config,
     * e.g. {@code propertyPath("info", "title")} on the OAS plugin yields {@code jsonapi4j.oas.info.title}. Use it
     * for every path reported to {@link PropertiesValidationResult}, so an error points at a key that can be
     * searched for as-is.
     *
     * @param path the property names, from this section's root down, indexes included (e.g. {@code "servers[0]"})
     * @return the config key prefixed with {@link #propertyPathPrefix()}
     */
    default String propertyPath(String... path) {
        return Stream.concat(
                Stream.of(propertyPathPrefix()),
                Arrays.stream(path)
        ).collect(Collectors.joining("."));
    }

}
