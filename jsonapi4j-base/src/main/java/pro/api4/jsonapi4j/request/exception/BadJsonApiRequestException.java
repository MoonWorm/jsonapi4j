package pro.api4.jsonapi4j.request.exception;

import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;

/**
 * @deprecated Use {@link ConstraintViolationException} instead.
 */
@Deprecated(forRemoval = true)
public class BadJsonApiRequestException extends ConstraintViolationException {

    public BadJsonApiRequestException(ErrorCode errorCode,
                                      String parameter,
                                      String message) {
        super(errorCode, message, parameter);
    }

}
