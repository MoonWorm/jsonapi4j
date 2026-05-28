package pro.api4.jsonapi4j.exception;

import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Base runtime exception for all JSON:API business-logic errors produced by the framework or
 * by application operation implementations.
 * <p>
 * Carries the HTTP status code, a machine-readable {@link ErrorCode}, and a human-readable
 * detail message. The framework's error handler chain converts instances (and subclasses) of
 * this exception into proper JSON:API {@code ErrorsDoc} responses automatically.
 *
 * @see JsonApiRequestValidationException
 * @see ResourceNotFoundException
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JsonApi4jException extends RuntimeException {

    /** The HTTP status code to send in the response (e.g. 400, 404, 500). */
    private final int httpStatus;

    /** A machine-readable error code identifying the type of error. */
    private final ErrorCode errorCode;

    /** A human-readable description of the specific error occurrence. */
    private final String detail;

    @Override
    public String getMessage() {
        return detail;
    }

    @Override
    public String toString() {
        return "Http status: " + httpStatus + ", error code: " + errorCode + ", detail: " + detail;
    }
}
