package io.jsonapi4j.exception;

import io.jsonapi4j.model.document.error.ErrorCode;
import lombok.Data;
import lombok.EqualsAndHashCode;


/**
 * Generic error for business logic.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class JsonApi4jException extends RuntimeException {

    private final int httpStatus;
    private final ErrorCode errorCode;
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
