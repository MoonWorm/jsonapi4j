package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.request.JsonApiRequest;

public interface ToOneRelationshipRepository<RESOURCE_DTO, RELATIONSHIP_DTO> extends
        ReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO>,
        UpdateToOneRelationshipOperation
{

    @Override
    default RELATIONSHIP_DTO readOne(JsonApiRequest relationshipRequest) {
        throw new OperationNotFoundException(OperationType.READ_TO_MANY_RELATIONSHIP);
    }

    @Override
    default void update(JsonApiRequest request) {
        throw new OperationNotFoundException(OperationType.UPDATE_TO_ONE_RELATIONSHIP);
    }

    @Override
    default void validate(JsonApiRequest request) {
        switch (request.getOperationType()) {
            case READ_TO_ONE_RELATIONSHIP -> validateReadToOne(request);
            case UPDATE_TO_ONE_RELATIONSHIP -> validateUpdateToOne(request);
        }
    }

    default void validateReadToOne(JsonApiRequest request) {
        ReadToOneRelationshipOperation.DEFAULT_VALIDATOR.accept(request);
    }

    default void validateUpdateToOne(JsonApiRequest request) {
        UpdateToOneRelationshipOperation.DEFAULT_VALIDATOR.accept(request);
    }

}
