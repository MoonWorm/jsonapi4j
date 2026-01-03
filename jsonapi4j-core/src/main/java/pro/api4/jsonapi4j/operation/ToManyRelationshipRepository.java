package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

public interface ToManyRelationshipRepository<RESOURCE_DTO, RELATIONSHIP_DTO> extends
        ReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>,
        UpdateToManyRelationshipOperation {

    @Override
    default CursorPageableResponse<RELATIONSHIP_DTO> readMany(JsonApiRequest relationshipRequest) {
        throw new OperationNotFoundException(OperationType.READ_TO_ONE_RELATIONSHIP);
    }

    @Override
    default void update(JsonApiRequest request) {
        throw new OperationNotFoundException(OperationType.UPDATE_TO_MANY_RELATIONSHIP);
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
