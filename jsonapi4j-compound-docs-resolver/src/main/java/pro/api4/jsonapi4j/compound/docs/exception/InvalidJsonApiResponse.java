package pro.api4.jsonapi4j.compound.docs.exception;

public class InvalidJsonApiResponse extends RuntimeException {

    public InvalidJsonApiResponse(String message) {
        super(message);
    }

    public InvalidJsonApiResponse(String message, Throwable cause) {
        super(message, cause);
    }
}
