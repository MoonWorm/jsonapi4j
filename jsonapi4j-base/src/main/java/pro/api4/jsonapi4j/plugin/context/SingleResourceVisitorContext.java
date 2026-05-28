package pro.api4.jsonapi4j.plugin.context;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.processor.single.resource.SingleResourceJsonApiContext;

/**
 * Context object for {@link pro.api4.jsonapi4j.plugin.SingleResourceVisitors} hook methods.
 * <p>
 * Carries all parameters needed by plugin visitors during the single-resource pipeline
 * (e.g. {@code GET /users/1}, {@code POST /users}).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 * @param <ATTRIBUTES>     the attributes object type
 */
@SuperBuilder
@Getter
public class SingleResourceVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends PluginVisitorContext<REQUEST> {

    /** The processing context containing all configured resolvers for this pipeline. */
    private final SingleResourceJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext;

    /**
     * The primary resource DTO returned by the operation.
     * Available from {@code onDataPostRetrieval} onwards; {@code null} during {@code onDataPreRetrieval}.
     */
    private final DATA_SOURCE_DTO dataSourceDto;

    /**
     * The single-resource document being built.
     * Available from {@code onRelationshipsPreRetrieval} onwards; {@code null} during data phases.
     */
    private final SingleResourceDoc<?> doc;

}
