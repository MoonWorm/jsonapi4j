package pro.api4.jsonapi4j.operation.exception;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.OperationType;
import lombok.Getter;

@Getter
public class OperationNotFoundException extends RuntimeException {

    public OperationNotFoundException(String message) {
        super(message);
    }

    public OperationNotFoundException(String path,
                                      String method,
                                      String message) {
        super("JSON:API operation can not be resolved for the path: " + path + ", and method: " + method + ". " + message);
    }

    public OperationNotFoundException(String path,
                                      String method) {
        this(path, method, "");
    }

    public OperationNotFoundException(OperationType operationType,
                                      ResourceType resourceType,
                                      RelationshipName relationshipName) {
        super("Implementation of the " + operationType.getName() + " operation is not found for the Relationship" + relationshipName.getName() + " of the Resource (" + resourceType.getType() + ").");
    }

    public OperationNotFoundException(OperationType operationType,
                                      ResourceType resourceType,
                                      RelationshipName relationshipName,
                                      Throwable cause) {
        super("Implementation of the " + operationType.getName() + " operation is not found for the Relationship" + relationshipName.getName() + " of the Resource (" + resourceType.getType() + ").", cause);
    }

    public OperationNotFoundException(OperationType operationType,
                                      ResourceType resourceType) {
        super("Implementation of the " + operationType.getName() + "  operation is not found for the Resource (" + resourceType.getType() + ").");
    }

    public OperationNotFoundException(OperationType operationType) {
        super("Implementation of the " + operationType.getName() + "  operation is not found");
    }

}
