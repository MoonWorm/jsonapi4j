package pro.api4.jsonapi4j.plugin;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.operation.Operation;

/**
 * SPI for extending the jsonapi4j processing pipeline with cross-cutting logic.
 * <p>
 * Plugins are registered once at startup and invoked by the processor engine at well-defined
 * phase hooks during every request. A plugin can:
 * <ul>
 *   <li>Extract metadata from domain objects at registration time
 *       ({@link #extractPluginInfoFromOperation}, {@link #extractPluginInfoFromResource},
 *       {@link #extractPluginInfoFromRelationship}) and receive it back during request processing
 *       via {@link JsonApiPluginInfo}.</li>
 *   <li>Intercept the processing pipeline before/after data retrieval and before/after
 *       relationship resolution by providing visitor implementations via the
 *       {@link #singleResourceVisitors()}, {@link #multipleResourcesVisitors()},
 *       {@link #toOneRelationshipVisitors()}, and {@link #toManyRelationshipVisitors()} methods.</li>
 * </ul>
 * <p>
 * Plugins are executed in ascending {@link #precedence()} order (lower value = higher priority).
 * Use the provided constants ({@link #HIGHEST_PRECEDENCE}, {@link #HIGH_PRECEDENCE},
 * {@link #LOW_PRECEDENCE}, {@link #LOWEST_PRECEDENCE}) as reference points.
 * <p>
 * Built-in plugins include the Access Control plugin ({@code jsonapi4j-ac-plugin}),
 * the Sparse Fieldsets plugin ({@code jsonapi4j-sf-plugin}), and the OpenAPI plugin
 * ({@code jsonapi4j-oas-plugin}).
 *
 * @see SingleResourceVisitors
 * @see MultipleResourcesVisitors
 * @see ToOneRelationshipVisitors
 * @see ToManyRelationshipVisitors
 */
public interface JsonApi4jPlugin {

    /** Highest execution priority. */
    int HIGHEST_PRECEDENCE = 0;
    /** High execution priority. */
    int HIGH_PRECEDENCE = 10;
    /** Low execution priority (default). */
    int LOW_PRECEDENCE = 100;
    /** Lowest execution priority. */
    int LOWEST_PRECEDENCE = Integer.MAX_VALUE;

    /**
     * Returns a unique human-readable identifier for this plugin, used in logging and diagnostics.
     *
     * @return plugin name, never {@code null}
     */
    String pluginName();

    /**
     * Controls whether this plugin participates in request processing.
     * When {@code false}, all visitors and info-extraction methods are skipped.
     * Default: {@code true}.
     *
     * @return {@code true} if the plugin is enabled
     */
    default boolean enabled() {
        return true;
    }

    /**
     * Controls the order in which this plugin's visitors are invoked relative to other plugins.
     * Lower values are invoked first. Use {@link #HIGHEST_PRECEDENCE}, {@link #HIGH_PRECEDENCE},
     * {@link #LOW_PRECEDENCE}, or {@link #LOWEST_PRECEDENCE} as reference points.
     * Default: {@link #LOW_PRECEDENCE}.
     *
     * @return plugin precedence value
     */
    default int precedence() {
        return LOW_PRECEDENCE;
    }

    /**
     * Extracts plugin-specific metadata from an operation at registration time.
     * The returned object is stored in {@link JsonApiPluginInfo#getOperationPluginInfo()}
     * and passed back to visitor methods during request processing.
     * <p>
     * Typical use: reading custom annotations placed on the operation class to drive
     * per-operation plugin behaviour (e.g. access-control rules, OAS tags).
     * Default: returns {@code null} (no info extracted).
     *
     * @param operation      the registered operation instance
     * @param operationClass the concrete class of the operation (may differ from {@code operation.getClass()} in proxy scenarios)
     * @return plugin-specific info object, or {@code null}
     */
    default Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        return null;
    }

    /**
     * Extracts plugin-specific metadata from a resource at registration time.
     * The returned object is stored in {@link JsonApiPluginInfo#getResourcePluginInfo()}
     * and passed back to visitor methods during request processing.
     * <p>
     * Default: returns {@code null} (no info extracted).
     *
     * @param resource the registered resource instance
     * @return plugin-specific info object, or {@code null}
     */
    default Object extractPluginInfoFromResource(Resource<?> resource) {
        return null;
    }

    /**
     * Extracts plugin-specific metadata from a relationship at registration time.
     * The returned object is stored in {@link JsonApiPluginInfo#getRelationshipPluginInfo()}
     * and passed back to visitor methods during request processing.
     * <p>
     * Default: returns {@code null} (no info extracted).
     *
     * @param relationship the registered relationship instance
     * @return plugin-specific info object, or {@code null}
     */
    default Object extractPluginInfoFromRelationship(Relationship<?> relationship) {
        return null;
    }

    /**
     * Returns the visitor implementation for single-resource processing hooks.
     * Override to provide non-default behaviour.
     *
     * @return {@link SingleResourceVisitors} instance, never {@code null}
     */
    default SingleResourceVisitors singleResourceVisitors() {
        return new SingleResourceVisitors() {
        };
    }

    /**
     * Returns the visitor implementation for multiple-resources processing hooks.
     * Override to provide non-default behaviour.
     *
     * @return {@link MultipleResourcesVisitors} instance, never {@code null}
     */
    default MultipleResourcesVisitors multipleResourcesVisitors() {
        return new MultipleResourcesVisitors() {
        };
    }

    /**
     * Returns the visitor implementation for to-one relationship processing hooks.
     * Override to provide non-default behaviour.
     *
     * @return {@link ToOneRelationshipVisitors} instance, never {@code null}
     */
    default ToOneRelationshipVisitors toOneRelationshipVisitors() {
        return new ToOneRelationshipVisitors() {
        };
    }

    /**
     * Returns the visitor implementation for to-many relationship processing hooks.
     * Override to provide non-default behaviour.
     *
     * @return {@link ToManyRelationshipVisitors} instance, never {@code null}
     */
    default ToManyRelationshipVisitors toManyRelationshipVisitors() {
        return new ToManyRelationshipVisitors() {
        };
    }

}
