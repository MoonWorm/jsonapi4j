package pro.api4.jsonapi4j.compound.docs.exception;

public class ErrorJsonApiResponseException extends RuntimeException {

    public ErrorJsonApiResponseException(String message) {
        super(message);
    }

    public ErrorJsonApiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
