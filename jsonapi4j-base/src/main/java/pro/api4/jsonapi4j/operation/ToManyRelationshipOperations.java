package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.response.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

public interface ToManyRelationshipOperations<RESOURCE_DTO, RELATIONSHIP_DTO> extends
        UpdateToManyRelationshipOperation, ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> {

    @Override
    default CursorPageableResponse<RELATIONSHIP_DTO> readMany(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.READ_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void update(JsonApiRequest request) {
        throw new OperationNotFoundException(
                OperationType.UPDATE_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

    @Override
    default void validate(JsonApiRequest request) {
        switch (request.getOperationType()) {
            case READ_TO_MANY_RELATIONSHIP -> validateReadToMany(request);
            case UPDATE_TO_MANY_RELATIONSHIP -> validateUpdateToMany(request);
        }
    }

    default void validateReadToMany(JsonApiRequest request) {
        ReadToManyRelationshipOperation.DEFAULT_VALIDATOR.accept(request);
    }

    default void validateUpdateToMany(JsonApiRequest request) {
        UpdateToManyRelationshipOperation.DEFAULT_VALIDATOR.accept(request);
    }

}
