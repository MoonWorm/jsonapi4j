package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

@Getter
public class JsonApiRequestValidationException extends JsonApi4jException {

    private final String detail;
    private final ErrorSources.Source source;

    public static JsonApiRequestValidationException withSource(JsonApiRequestValidationException e,
                                                               ErrorSources.Source source) {
        return new JsonApiRequestValidationException(e.getErrorCode(), e.getDetail(), source);
    }

    public JsonApiRequestValidationException(ErrorCode errorCode,
                                             String detail) {
        this(errorCode, detail, null);
    }

    public JsonApiRequestValidationException(ErrorCode errorCode,
                                             String detail,
                                             ErrorSources.Source source) {
        super(HttpStatusCodes.SC_400_BAD_REQUEST.getCode(), errorCode, source != null ? source + ":" + detail : detail);
        this.detail = detail;
        this.source = source;
    }

    public JsonApiRequestValidationException(String detail,
                                             ErrorSources.Source source) {
        this(DefaultErrorCodes.GENERIC_REQUEST_ERROR, detail, source);
    }

    public JsonApiRequestValidationException(String detail) {
        this(detail, null);
    }

}
