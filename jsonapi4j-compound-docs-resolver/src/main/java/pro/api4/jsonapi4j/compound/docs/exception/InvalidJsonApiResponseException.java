package pro.api4.jsonapi4j.compound.docs.exception;

public class InvalidJsonApiResponseException extends RuntimeException {

    public InvalidJsonApiResponseException(String message) {
        super(message);
    }

    public InvalidJsonApiResponseException(String message, Throwable cause) {
        super(message, cause);
    }
}
