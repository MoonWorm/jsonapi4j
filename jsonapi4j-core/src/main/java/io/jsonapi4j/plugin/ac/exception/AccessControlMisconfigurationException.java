package io.jsonapi4j.plugin.ac.exception;

public class AccessControlMisconfigurationException extends RuntimeException {

    public AccessControlMisconfigurationException(String message) {
        super(message);
    }

    public AccessControlMisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
