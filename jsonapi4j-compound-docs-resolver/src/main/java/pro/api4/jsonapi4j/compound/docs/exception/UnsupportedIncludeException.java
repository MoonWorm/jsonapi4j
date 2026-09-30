package pro.api4.jsonapi4j.compound.docs.exception;

/**
 * The request asks for an {@code include} path the resolver does not support - e.g. one deeper than the configured
 * maximum. A client error: the request has to change, a retry returns the same.
 */
public class UnsupportedIncludeException extends RuntimeException {

    public UnsupportedIncludeException(String message) {
        super(message);
    }

}
