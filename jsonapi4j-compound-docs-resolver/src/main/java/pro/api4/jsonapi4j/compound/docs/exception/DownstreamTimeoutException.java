package pro.api4.jsonapi4j.compound.docs.exception;

/**
 * A downstream call made to resolve included resources timed out - connecting, or waiting for the response.
 */
public class DownstreamTimeoutException extends ErrorJsonApiResponseException {

    public DownstreamTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

}
