package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * Can be explicitly thrown from the operation if client sent an invalid cursor value.
 */
@Getter
public class InvalidCursorException extends JsonApiRequestValidationException {

    private final String cursor;

    public InvalidCursorException(String cursor, String message) {
        super(DefaultErrorCodes.INVALID_CURSOR, message, ErrorSources.url().queryParams().cursor());
        this.cursor = cursor;
    }

    public InvalidCursorException(String cursor) {
        this(cursor, "Invalid cursor value: " + cursor);
    }

}
