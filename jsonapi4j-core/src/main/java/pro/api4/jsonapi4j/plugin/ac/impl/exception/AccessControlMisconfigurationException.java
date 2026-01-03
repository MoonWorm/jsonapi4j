package pro.api4.jsonapi4j.plugin.ac.impl.exception;

public class AccessControlMisconfigurationException extends RuntimeException {

    public AccessControlMisconfigurationException(String message) {
        super(message);
    }

    public AccessControlMisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
