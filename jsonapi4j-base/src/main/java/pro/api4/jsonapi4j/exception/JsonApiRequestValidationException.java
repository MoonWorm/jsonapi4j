package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * Exception representing a client request validation failure (HTTP 400 Bad Request).
 * <p>
 * Thrown by the framework's built-in validators (e.g. for invalid pagination parameters,
 * malformed payloads) and may also be thrown explicitly by operation implementations when
 * incoming request data fails business-logic validation.
 * <p>
 * Carries an optional {@link pro.api4.jsonapi4j.operation.validation.ErrorSources.Source}
 * that maps the error to the specific request location (JSON Pointer or query parameter name)
 * as required by the JSON:API specification's error source format.
 *
 * @see CompositeJsonApiRequestValidationException
 * @see pro.api4.jsonapi4j.operation.validation.ErrorSources
 */
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
