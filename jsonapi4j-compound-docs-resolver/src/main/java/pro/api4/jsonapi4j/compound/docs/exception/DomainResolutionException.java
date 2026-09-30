package pro.api4.jsonapi4j.compound.docs.exception;

/**
 * No route to fetch an included resource type from - e.g. the type is not mapped and there is no fallback. A
 * configuration problem rather than a transient failure.
 */
public class DomainResolutionException extends RuntimeException {

    public DomainResolutionException(String message) {
        super(message);
    }

    public DomainResolutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
