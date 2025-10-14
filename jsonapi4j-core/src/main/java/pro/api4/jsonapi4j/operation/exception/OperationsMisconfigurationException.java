package pro.api4.jsonapi4j.operation.exception;

public class OperationsMisconfigurationException extends RuntimeException {

    public OperationsMisconfigurationException(String message) {
        super(message);
    }

    public OperationsMisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
