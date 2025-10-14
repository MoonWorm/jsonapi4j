package pro.api4.jsonapi4j.compound.docs.exception;

public class ErrorJsonApiResponse extends RuntimeException {

    public ErrorJsonApiResponse(String message) {
        super(message);
    }

    public ErrorJsonApiResponse(String message, Throwable cause) {
        super(message, cause);
    }
}
