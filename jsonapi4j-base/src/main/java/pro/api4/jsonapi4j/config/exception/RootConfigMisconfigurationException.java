package pro.api4.jsonapi4j.config.exception;

/**
 * Thrown at startup when the {@code JsonApi4jBuilder} detects a misconfiguration in the root
 * {@code jsonapi4j} configuration, such as a root path that cannot be mounted or a validation
 * limit that rejects every request.
 * <p>
 * This is a programmer error and should never occur at runtime in a correctly configured
 * application.
 */
public class RootConfigMisconfigurationException extends RuntimeException {

    public RootConfigMisconfigurationException(String message) {
        super(message);
    }

    public RootConfigMisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
