package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.operation.OperationType;

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
        ProfileAwareRequest {

    /**
     * @return this operation {@link OperationType} that is basically represents one of the available JSON:API
     * operations.
     */
    OperationType getOperationType();

}
