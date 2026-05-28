package pro.api4.jsonapi4j.domain.exception;

/**
 * Thrown at startup when the {@code DomainRegistry} detects a misconfiguration in the declared
 * resources or relationships, such as missing or conflicting annotations, invalid relationship
 * references, or incompatible type parameters.
 * <p>
 * This is a programmer error and should never occur at runtime in a correctly configured
 * application.
 */
public class DomainMisconfigurationException extends RuntimeException {

    public DomainMisconfigurationException(String message) {
        super(message);
    }

    public DomainMisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
