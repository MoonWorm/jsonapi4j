package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

public interface ToManyRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO> extends
        ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>,
        UpdateToManyRelationshipOperation,
        AddToManyRelationshipOperation,
        DeleteToManyRelationshipOperation {

    @Override
    default PaginationAwareResponse<RELATIONSHIP_DTO> readMany(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.READ_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void update(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.UPDATE_TO_MANY_RELATIONSHIPS,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void add(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.ADD_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void delete(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.DELETE_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void validate(JsonApiRequest request) {
        switch (request.getOperationType()) {
            case READ_TO_MANY_RELATIONSHIP -> validateReadToMany(request);
            case UPDATE_TO_MANY_RELATIONSHIPS -> validateUpdateToMany(request);
            case ADD_TO_MANY_RELATIONSHIP -> validateAddToMany(request);
            case DELETE_TO_MANY_RELATIONSHIP -> validateDeleteFromToMany(request);
        }
    }

    default void validateReadToMany(JsonApiRequest request) {
    }

    default void validateUpdateToMany(JsonApiRequest request) {
    }

    default void validateAddToMany(JsonApiRequest request) {
    }

    default void validateDeleteFromToMany(JsonApiRequest request) {
    }

}
