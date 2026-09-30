package pro.api4.jsonapi4j.compound.docs.exception;

/**
 * A downstream call made to resolve included resources failed: a non-200 response, a connection error, or a response
 * that is not a JSON:API document.
 */
public class ErrorJsonApiResponseException extends RuntimeException {

    public ErrorJsonApiResponseException(String message) {
        super(message);
    }

    public ErrorJsonApiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
