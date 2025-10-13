package io.jsonapi4j.operation.exception;

import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.domain.ResourceType;
import io.jsonapi4j.operation.OperationType;
import lombok.Getter;

@Getter
public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String message) {
        super(message);
    }

    public OperationNotFoundException(String path,
                                      String method,
                                      String message) {
        this("JSON:API operation can not be resolved for the path: " + path + ", and method: " + method + ". " + message);
    }

    public OperationNotFoundException(String path, String method) {
        this(path, method, "");
    }

    public OperationNotFoundException(OperationType operationType,
                                      ResourceType resourceType,
                                      RelationshipName relationshipName) {
        this("Implementation of the " + operationType.getName() + " operation is not found for the Relationship" + relationshipName.getName() + " of the Resource (" + resourceType.getType() + ").");
    }

    public OperationNotFoundException(OperationType operationType,
                                      ResourceType resourceType) {
        this("Implementation of the " + operationType.getName() + "  operation is not found for the Resource (" + resourceType.getType() + ").");
    }

}
