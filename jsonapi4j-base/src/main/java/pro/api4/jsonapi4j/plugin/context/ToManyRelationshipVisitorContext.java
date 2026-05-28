package pro.api4.jsonapi4j.plugin.context;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.processor.multi.relationship.ToManyRelationshipsJsonApiContext;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

/**
 * Context object for {@link pro.api4.jsonapi4j.plugin.ToManyRelationshipVisitors} hook methods.
 * <p>
 * Carries all parameters needed by plugin visitors during the to-many relationship pipeline
 * (e.g. {@code GET /users/1/relationships/citizenships}).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream relationship DTO type
 */
@SuperBuilder
@Getter
public class ToManyRelationshipVisitorContext<REQUEST, DATA_SOURCE_DTO>
        extends PluginVisitorContext<REQUEST> {

    /** The processing context containing all configured resolvers for this pipeline. */
    private final ToManyRelationshipsJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext;

    /**
     * The paginated response returned by the operation.
     * Available during {@code onDataPostRetrieval}; {@code null} during {@code onDataPreRetrieval}.
     */
    private final PaginationAwareResponse<DATA_SOURCE_DTO> paginationAwareResponse;

    /**
     * The built to-many relationships document.
     * Available during {@code onDataPostRetrieval}; {@code null} during {@code onDataPreRetrieval}.
     */
    private final ToManyRelationshipsDoc doc;

}
