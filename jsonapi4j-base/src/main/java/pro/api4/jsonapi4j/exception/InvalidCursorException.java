package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * Signals that the client supplied an invalid or malformed {@code page[cursor]} value (HTTP 400).
 * <p>
 * Thrown automatically by {@link pro.api4.jsonapi4j.response.pagination.LimitOffsetToCursorAdapter}
 * when cursor decoding fails, or can be thrown explicitly from an operation if custom cursor
 * validation is needed. Carries the raw cursor string that was received.
 */
@Getter
public class InvalidCursorException extends JsonApiRequestValidationException {

    private final String cursor;

    public InvalidCursorException(String cursor, String message) {
        super(DefaultErrorCodes.INVALID_CURSOR, message, ErrorSources.parameter().cursor());
        this.cursor = cursor;
    }

    public InvalidCursorException(String cursor) {
        this(cursor, "Invalid cursor value: " + cursor);
    }

}
