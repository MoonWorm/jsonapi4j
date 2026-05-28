package pro.api4.jsonapi4j.plugin;

import lombok.Data;

/**
 * Carries the plugin-specific metadata objects extracted at startup time for a given operation,
 * its parent resource, and (for relationship operations) its relationship.
 * <p>
 * An instance is created by the framework for each registered plugin and passed to every visitor
 * method call, giving the plugin access to the pre-extracted metadata without re-reading
 * annotations on every request.
 *
 * @see JsonApi4jPlugin#extractPluginInfoFromOperation
 * @see JsonApi4jPlugin#extractPluginInfoFromResource
 * @see JsonApi4jPlugin#extractPluginInfoFromRelationship
 */
@Data
public class JsonApiPluginInfo {

    /** Metadata extracted from the operation class by {@link JsonApi4jPlugin#extractPluginInfoFromOperation}. */
    private final Object operationPluginInfo;

    /** Metadata extracted from the resource instance by {@link JsonApi4jPlugin#extractPluginInfoFromResource}. */
    private final Object resourcePluginInfo;

    /** Metadata extracted from the relationship instance by {@link JsonApi4jPlugin#extractPluginInfoFromRelationship}. {@code null} for resource-level operations. */
    private final Object relationshipPluginInfo;

}
