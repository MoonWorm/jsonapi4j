package pro.api4.jsonapi4j.plugin.context;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.processor.multi.resource.MultipleResourcesJsonApiContext;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

/**
 * Context object for {@link pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors} hook methods.
 * <p>
 * Carries all parameters needed by plugin visitors during the multiple-resources pipeline
 * (e.g. {@code GET /users}).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream resource DTO type
 * @param <ATTRIBUTES>     the attributes object type
 */
@SuperBuilder
@Getter
public class MultipleResourcesVisitorContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES>
        extends PluginVisitorContext<REQUEST> {

    /** The processing context containing all configured resolvers for this pipeline. */
    private final MultipleResourcesJsonApiContext<REQUEST, DATA_SOURCE_DTO, ATTRIBUTES> jsonApiContext;

    /**
     * The paginated response returned by the operation.
     * Available from {@code onDataPostRetrieval} onwards; {@code null} during {@code onDataPreRetrieval}.
     */
    private final PaginationAwareResponse<DATA_SOURCE_DTO> paginationAwareResponse;

    /**
     * The multiple-resources document being built.
     * Available from {@code onRelationshipsPreRetrieval} onwards; {@code null} during data phases.
     */
    private final MultipleResourcesDoc<?> doc;

}
