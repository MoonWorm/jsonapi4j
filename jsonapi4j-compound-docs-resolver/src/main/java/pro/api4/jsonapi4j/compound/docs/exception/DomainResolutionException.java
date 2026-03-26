package pro.api4.jsonapi4j.compound.docs.exception;

public class DomainResolutionException extends RuntimeException {

    public DomainResolutionException(String message) {
        super(message);
    }

    public DomainResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
