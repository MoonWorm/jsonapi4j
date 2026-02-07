package pro.api4.jsonapi4j.processor.exception;

/**
 * Can be explicitly thrown from the operation when an error happened during converting downstream model into JSON:API
 * attributes.
 */
public class MappingException extends RuntimeException {

    public MappingException(String message) {
        super(message);
    }

    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }

}
