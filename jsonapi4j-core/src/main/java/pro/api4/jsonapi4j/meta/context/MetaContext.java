package pro.api4.jsonapi4j.meta.context;

import lombok.*;
import pro.api4.jsonapi4j.config.Integration;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.RawConfigAccessor;
import pro.api4.jsonapi4j.domain.DomainRegistry;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import static pro.api4.jsonapi4j.meta.context.FrameworkVersionResolver.resolveFrameworkVersion;
import static pro.api4.jsonapi4j.meta.context.JavaVersionResolver.resolveJavaVersion;

/**
 * Immutable, integration-supplied descriptor for the built-in introspection ("meta") API.
 * <p>
 * Each host integration (Spring Boot, Quarkus, Servlet) assembles a {@code MetaContext} from its own
 * configuration and hands it to the registry builders via {@code withMeta(...)}. The meta resources read
 * it at request time to render the {@code state} / {@code config} resources, and {@link pro.api4.jsonapi4j.JsonApi4jReportGenerator}
 * reads it to decide whether to print live introspection URLs (rendered relative to root path).
 *
 * @see DomainRegistry.MetaDomain
 */
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
public class MetaContext {

    /**
     * Which host integration is running.
     */
    private final Integration integration;

    /**
     * The framework version (single source of truth: {@code <revision>} in the root pom).
     */
    private final String frameworkVersion;

    /**
     * The running Java runtime version.
     */
    private final String javaVersion;

    /**
     * Structural, non-secret effective configuration exposed by the {@code config} resource
     * (e.g. {@code rootPath}, plugin-enabled flags, validation limits); an unmodifiable, insertion-ordered
     * snapshot. Never put secrets here.
     */
    private final Map<String, Object> config;

    /**
     * Assembles a {@code MetaContext} from the effective config subtree: stores a defensive copy of the config
     * (the top-level {@link JsonApi4jProperties#ROOT_PATH_PROPERTY} backs {@link #getRootPath()}) and fills the
     * {@link #frameworkVersion} and {@link #javaVersion} from the runtime.
     *
     * @param config      the effective, normalized {@code jsonapi4j.*} config subtree (top-level
     *                    {@link JsonApi4jProperties#ROOT_PATH_PROPERTY} supplies the root path); may be {@code null}
     * @param integration the host integration flavor
     * @return the assembled context
     */
    public static MetaContext of(Map<String, Object> config, Integration integration) {
        Map<String, Object> effectiveConfig = config == null ? new LinkedHashMap<>() : new LinkedHashMap<>(config);
        return new MetaContext(
                integration,
                resolveFrameworkVersion(),
                resolveJavaVersion(),
                Collections.unmodifiableMap(effectiveConfig)
        );
    }

    /**
     * @return the configured JSON:API root path (e.g. {@code /jsonapi}), falling back to
     * {@link JsonApi4jProperties#DEFAULT_ROOT_PATH}; introspection links are rendered relative to it.
     */
    public String getRootPath() {
        return new RawConfigAccessor(config)
                .strValue(JsonApi4jProperties.ROOT_PATH_PROPERTY)
                .orElse(JsonApi4jProperties.DEFAULT_ROOT_PATH);
    }

}
