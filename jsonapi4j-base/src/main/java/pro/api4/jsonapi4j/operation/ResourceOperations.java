package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.response.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

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
    default CursorPageableResponse<RESOURCE_DTO> readPage(JsonApiRequest request) {
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

    default void validateReadById(JsonApiRequest request) {
        ReadResourceByIdOperation.DEFAULT_VALIDATOR.accept(request);
    }

    default void validateReadMultiple(JsonApiRequest request) {
        ReadMultipleResourcesOperation.DEFAULT_VALIDATOR.accept(request);
    }

    default void validateCreate(JsonApiRequest request) {
        CreateResourceOperation.DEFAULT_VALIDATOR.accept(request);
    }

    default void validateUpdate(JsonApiRequest request) {
        UpdateResourceOperation.DEFAULT_VALIDATOR.accept(request);
    }

    default void validateDelete(JsonApiRequest request) {
        DeleteResourceOperation.DEFAULT_VALIDATOR.accept(request);
    }

}
