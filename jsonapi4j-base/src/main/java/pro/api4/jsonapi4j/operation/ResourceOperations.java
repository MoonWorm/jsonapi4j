package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

/**
 * Composite operation interface that covers all five CRUD operations for a JSON:API resource.
 * <p>
 * Implement this interface instead of the individual operation interfaces when you want
 * to group all resource operations in a single class. Each operation method throws
 * {@link pro.api4.jsonapi4j.operation.exception.OperationNotFoundException} by default, so you only need
 * to override the operations your resource actually supports.
 * <p>
 * The {@link #validate(pro.api4.jsonapi4j.request.JsonApiRequest)} method automatically
 * dispatches to the appropriate per-operation validation hook based on the operation type.
 * Override any of the {@code validateXxx} methods to add operation-specific validation logic:
 * <ul>
 *   <li>{@link #validateReadById(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 *   <li>{@link #validateReadMultiple(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 *   <li>{@link #validateCreate(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 *   <li>{@link #validateUpdate(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 *   <li>{@link #validateDelete(pro.api4.jsonapi4j.request.JsonApiRequest)}</li>
 * </ul>
 *
 * @param <RESOURCE_DTO> the downstream object type for this resource
 * @see ReadResourceByIdOperation
 * @see ReadMultipleResourcesOperation
 * @see CreateResourceOperation
 * @see UpdateResourceOperation
 * @see DeleteResourceOperation
 */
public interface ResourceOperations<RESOURCE_DTO> extends
        ReadResourceByIdOperation<RESOURCE_DTO>,
        ReadMultipleResourcesOperation<RESOURCE_DTO>,
        CreateResourceOperation<RESOURCE_DTO>,
        UpdateResourceOperation,
        DeleteResourceOperation
{

    @Override
    default RESOURCE_DTO readById(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.READ_RESOURCE_BY_ID,
                request.getTargetResourceType()
        );
    }

    @Override
    default PaginationAwareResponse<RESOURCE_DTO> readPage(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.READ_MULTIPLE_RESOURCES,
                request.getTargetResourceType()
        );
    }

    @Override
    default RESOURCE_DTO create(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.CREATE_RESOURCE,
                request.getTargetResourceType()
        );
    }

    @Override
    default void update(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.UPDATE_RESOURCE,
                request.getTargetResourceType()
        );
    }

    @Override
    default void delete(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.DELETE_RESOURCE,
                request.getTargetResourceType()
        );
    }

    @Override
    default void validate(JsonApiRequest request) {
        switch (request.getOperationType()) {
            case READ_RESOURCE_BY_ID -> validateReadById(request);
            case READ_MULTIPLE_RESOURCES -> validateReadMultiple(request);
            case CREATE_RESOURCE -> validateCreate(request);
            case UPDATE_RESOURCE -> validateUpdate(request);
            case DELETE_RESOURCE -> validateDelete(request);
        }
    }

    /**
     * Validation hook called before {@link #readById(JsonApiRequest)}.
     * Override to add validation logic specific to read-by-id requests.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateReadById(JsonApiRequest request) {
    }

    /**
     * Validation hook called before {@link #readPage(JsonApiRequest)}.
     * Override to add validation logic specific to read-multiple requests (filters, pagination, etc.).
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateReadMultiple(JsonApiRequest request) {
    }

    /**
     * Validation hook called before {@link #create(JsonApiRequest)}.
     * Override to add payload and input validation logic specific to resource creation.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateCreate(JsonApiRequest request) {
    }

    /**
     * Validation hook called before {@link #update(JsonApiRequest)}.
     * Override to add payload and input validation logic specific to resource update.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateUpdate(JsonApiRequest request) {
    }

    /**
     * Validation hook called before {@link #delete(JsonApiRequest)}.
     * Override to add validation logic specific to resource deletion.
     * By default, no validation is performed.
     *
     * @param request incoming {@link JsonApiRequest}
     */
    default void validateDelete(JsonApiRequest request) {
    }

}
