package pro.api4.jsonapi4j.operation.exception;

/**
 * Thrown at startup when the {@code OperationsRegistry} detects a misconfiguration in the
 * registered operations, such as duplicate operation registrations, missing required annotations,
 * or incompatible operation/resource type pairings.
 * <p>
 * This is a programmer error and should never occur at runtime in a correctly configured
 * application.
 */
public class OperationsMisconfigurationException extends RuntimeException {

    public OperationsMisconfigurationException(String message) {
        super(message);
    }

    public OperationsMisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
