package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * Used by the framework. Also, can be explicitly thrown from the operation if needed but not recommended.
 */
@Getter
public class InvalidLimitException extends JsonApiRequestValidationException {

    private final Long limit;

    public InvalidLimitException(Long limit, String message) {
        super(DefaultErrorCodes.INVALID_LIMIT, message, ErrorSources.url().queryParams().limit());
        this.limit = limit;
    }

    public InvalidLimitException(Long limit) {
        this(limit, "Invalid limit value: " + limit + ". Must be a number.");
    }

}
