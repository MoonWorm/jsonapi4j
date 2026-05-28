package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.operation.OperationType;

/**
 * Composite interface representing a fully parsed JSON:API HTTP request.
 *
 * <p>Aggregates all request-aspect mixins into a single type that is passed through the
 * processing pipeline: resource identification, relationship targeting, pagination, filtering,
 * sorting, sparse fieldsets, includes, payload deserialization, custom query params, content
 * negotiation (extensions and profiles), and HTTP headers.
 *
 * <p>Implementations are typically created by the servlet layer from the incoming HTTP request
 * and are immutable after construction.
 *
 * @see ResourceAwareRequest
 * @see RelationshipAwareRequest
 * @see PaginationAwareRequest
 * @see FiltersAwareRequest
 * @see SortAwareRequest
 * @see IncludeAwareRequest
 * @see SparseFieldsetsAwareRequest
 * @see PayloadAwareRequest
 */
public interface JsonApiRequest extends
        ResourceAwareRequest,
        RelationshipAwareRequest,
        PaginationAwareRequest,
        IncludeAwareRequest,
        FiltersAwareRequest,
        SortAwareRequest,
        SparseFieldsetsAwareRequest,
        CustomQueryParamsAwareRequest,
        PayloadAwareRequest,
        ExtensionAwareRequest,
        ProfileAwareRequest,
        HeadersAwareRequest {

    /**
     * @return this operation {@link OperationType} that is basically represents one of the available JSON:API
     * operations.
     */
    OperationType getOperationType();

}
