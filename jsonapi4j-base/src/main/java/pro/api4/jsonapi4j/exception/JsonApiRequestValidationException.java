package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

@Getter
public class JsonApiRequestValidationException extends JsonApi4jException {

    private final String detail;
    private final ErrorSources.ParameterPath parameter;

    public static JsonApiRequestValidationException withParameter(JsonApiRequestValidationException e,
                                                                  ErrorSources.ParameterPath parameter) {
        return new JsonApiRequestValidationException(e.getErrorCode(), e.getDetail(), parameter);
    }

    public JsonApiRequestValidationException(ErrorCode errorCode,
                                             String detail,
                                             ErrorSources.ParameterPath parameter) {
        super(HttpStatusCodes.SC_400_BAD_REQUEST.getCode(), errorCode, parameter != null ? parameter + ":" + detail : detail);
        this.detail = detail;
        this.parameter = parameter;
    }

    public JsonApiRequestValidationException(String detail,
                                             ErrorSources.ParameterPath parameter) {
        this(DefaultErrorCodes.GENERIC_REQUEST_ERROR, detail, parameter);
    }

    public JsonApiRequestValidationException(String detail) {
        this(detail, null);
    }

}
