package pro.api4.jsonapi4j.request.exception;

import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * @deprecated Use {@link JsonApiRequestValidationException} instead.
 */
@Deprecated(forRemoval = true)
public class BadJsonApiRequestException extends JsonApiRequestValidationException {

    public BadJsonApiRequestException(ErrorCode errorCode,
                                      ErrorSources.ParameterPath parameter,
                                      String message) {
        super(errorCode, message, parameter);
    }

}
