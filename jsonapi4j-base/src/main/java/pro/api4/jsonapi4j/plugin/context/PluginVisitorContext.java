package pro.api4.jsonapi4j.plugin.context;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.plugin.JsonApiPluginInfo;

/**
 * Base context object passed to every plugin visitor method.
 * <p>
 * Aggregates the common parameters that are available in <em>all</em> visitor phases:
 * the current request, operation metadata, and pre-extracted plugin-specific info.
 * <p>
 * Concrete subclasses add pipeline-specific fields (the {@code JsonApiContext},
 * the data-source DTO, and the document) with appropriate types for each pipeline
 * (single resource, multiple resources, to-one relationship, to-many relationship).
 *
 * @param <REQUEST> the request type
 */
@SuperBuilder
@Getter
public abstract class PluginVisitorContext<REQUEST> {

    /** The current request (may have been mutated by a prior plugin in the chain). */
    private final REQUEST request;

    /** Metadata about the operation being processed (type, resource type, relationship name, etc.). */
    private final OperationMeta operationMeta;

    /** Pre-extracted plugin-specific metadata for the current operation, resource, and relationship. */
    private final JsonApiPluginInfo pluginInfo;

}
