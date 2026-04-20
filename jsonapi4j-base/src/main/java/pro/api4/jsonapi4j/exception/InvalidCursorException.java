package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

/**
 * Can be explicitly thrown from the operation if client sent an invalid cursor value.
 */
@Getter
public class InvalidCursorException extends ConstraintViolationException {

    private final String cursor;

    public InvalidCursorException(String cursor, String message) {
        super(DefaultErrorCodes.INVALID_CURSOR, message, "page[cursor]");
        this.cursor = cursor;
    }

    public InvalidCursorException(String cursor) {
        this(cursor, "Invalid cursor value: " + cursor);
    }

}
