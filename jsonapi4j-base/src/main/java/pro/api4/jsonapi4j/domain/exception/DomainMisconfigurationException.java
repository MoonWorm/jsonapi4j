package pro.api4.jsonapi4j.domain.exception;

public class DomainMisconfigurationException extends RuntimeException {

    public DomainMisconfigurationException(String message) {
        super(message);
    }

    public DomainMisconfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
