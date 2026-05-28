package pro.api4.jsonapi4j.operation.exception;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.OperationType;
import lombok.Getter;

/**
 * Thrown when no registered operation implementation can be resolved for an incoming request.
 * <p>
 * The framework throws this at dispatch time if the {@code OperationsRegistry} contains no
 * matching operation for the given HTTP method, path, and resource/relationship type. It is also
 * thrown by default by the {@link pro.api4.jsonapi4j.operation.ResourceOperations} and
 * {@link pro.api4.jsonapi4j.operation.ToOneRelationshipOperations} /
 * {@link pro.api4.jsonapi4j.operation.ToManyRelationshipOperations} default method implementations
 * for operations that the application has not overridden, signalling that the operation is not
 * supported (HTTP 405 or 404 depending on the context).
 */
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
