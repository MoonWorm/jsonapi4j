package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * Signals that the {@code page[limit]} query parameter value is invalid (HTTP 400).
 * <p>
 * Thrown automatically by the framework when the limit parameter cannot be parsed as a number.
 * Can also be thrown explicitly from an operation, though that is rarely needed. Carries the
 * raw limit value that was received.
 */
@Getter
public class InvalidLimitException extends JsonApiRequestValidationException {

    private final Long limit;

    public InvalidLimitException(Long limit, String message) {
        super(DefaultErrorCodes.INVALID_LIMIT, message, ErrorSources.parameter().limit());
        this.limit = limit;
    }

    public InvalidLimitException(Long limit) {
        this(limit, "Invalid limit value: " + limit + ". Must be a number.");
    }

}
