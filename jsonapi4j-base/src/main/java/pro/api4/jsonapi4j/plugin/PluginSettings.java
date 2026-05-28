package pro.api4.jsonapi4j.plugin;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import pro.api4.jsonapi4j.operation.OperationMeta;

/**
 * Bundles together the runtime context that a {@link JsonApi4jPlugin} needs in order to
 * process a single operation invocation: which operation is being executed, the plugin instance
 * itself, and the pre-extracted plugin-specific metadata.
 * <p>
 * Instances are built by the framework per (plugin, operation) pair at request time and are
 * not intended to be constructed by application code.
 *
 * @see JsonApi4jPlugin
 * @see JsonApiPluginInfo
 * @see OperationMeta
 */
@Builder
@Getter
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PluginSettings {

    /** Metadata about the operation being processed (type, resource type, relationship name, etc.). */
    private final OperationMeta operationMeta;

    /** The plugin instance that these settings belong to. */
    private final JsonApi4jPlugin plugin;

    /** Pre-extracted plugin-specific info objects for the current operation, resource, and relationship. */
    private final JsonApiPluginInfo info;

}
