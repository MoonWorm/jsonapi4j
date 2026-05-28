package pro.api4.jsonapi4j.operation;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;

import java.util.Map;

/**
 * Immutable metadata snapshot describing a registered operation and its context.
 * <p>
 * Built by the framework at startup for each registered operation and passed to every
 * plugin visitor method call, giving plugins read-only access to key operation attributes
 * without requiring reflection at runtime.
 *
 * @see pro.api4.jsonapi4j.plugin.JsonApi4jPlugin
 * @see pro.api4.jsonapi4j.plugin.PluginSettings
 */
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Getter
@ToString
@EqualsAndHashCode
public class OperationMeta {

    /** The class under which this operation was registered in the {@code OperationsRegistry}. */
    private final Class<?> registeredAs;

    /** The resource type this operation belongs to (e.g. {@code new ResourceType("users")}). */
    private final ResourceType resourceType;

    /**
     * The relationship name this operation belongs to.
     * {@code null} for resource-level operations (CRUD).
     */
    private final RelationshipName relationshipName;

    /** The specific operation type (read-by-id, create, read-to-many-relationship, etc.). */
    private final OperationType operationType;

    /**
     * A map of plugin-specific info objects keyed by plugin name.
     * Each entry is the result of the corresponding plugin's
     * {@link pro.api4.jsonapi4j.plugin.JsonApi4jPlugin#extractPluginInfoFromOperation} call.
     */
    private final Map<String, Object> pluginInfo;
}
