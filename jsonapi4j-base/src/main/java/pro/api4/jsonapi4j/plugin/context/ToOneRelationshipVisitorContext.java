package pro.api4.jsonapi4j.plugin.context;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.processor.single.relationship.ToOneRelationshipJsonApiContext;

/**
 * Context object for {@link pro.api4.jsonapi4j.plugin.ToOneRelationshipVisitors} hook methods.
 * <p>
 * Carries all parameters needed by plugin visitors during the to-one relationship pipeline
 * (e.g. {@code GET /users/1/relationships/placeOfBirth}).
 *
 * @param <REQUEST>        the request type
 * @param <DATA_SOURCE_DTO> the downstream relationship DTO type
 */
@SuperBuilder
@Getter
public class ToOneRelationshipVisitorContext<REQUEST, DATA_SOURCE_DTO>
        extends PluginVisitorContext<REQUEST> {

    /** The processing context containing all configured resolvers for this pipeline. */
    private final ToOneRelationshipJsonApiContext<REQUEST, DATA_SOURCE_DTO> jsonApiContext;

    /**
     * The relationship DTO returned by the operation.
     * Available during {@code onDataPostRetrieval}; {@code null} during {@code onDataPreRetrieval}.
     */
    private final DATA_SOURCE_DTO dataSourceDto;

    /**
     * The built to-one relationship document.
     * Available during {@code onDataPostRetrieval}; {@code null} during {@code onDataPreRetrieval}.
     */
    private final ToOneRelationshipDoc doc;

}
