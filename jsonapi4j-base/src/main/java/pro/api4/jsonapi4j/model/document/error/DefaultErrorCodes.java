package pro.api4.jsonapi4j.model.document.error;

/**
 * Built-in {@link ErrorCode} constants covering the full range of errors produced by the
 * framework's validation, processing, and HTTP layers.
 * <p>
 * These codes are used by the framework's built-in validators, error handlers, and
 * exception types (e.g. {@link pro.api4.jsonapi4j.exception.JsonApiRequestValidationException},
 * {@link pro.api4.jsonapi4j.exception.InvalidCursorException}), but are equally available
 * for use in application operation implementations — there is no need to define custom
 * {@link ErrorCode} enums for errors that are already covered here.
 */
public enum DefaultErrorCodes implements ErrorCode {

    GENERIC_REQUEST_ERROR("GENERIC_REQUEST_ERROR"),
    MISSING_REQUIRED_PARAMETER("MISSING_REQUIRED_PARAMETER"),
    MISSING_REQUIRED_HEADER("MISSING_REQUIRED_HEADER"),
    INVALID_ENUM_VALUE("INVALID_ENUM_VALUE"),
    VALUE_IS_ABSENT("VALUE_IS_ABSENT"),
    VALUE_IS_NOT_ABSENT("VALUE_IS_NOT_ABSENT"),
    VALUE_EMPTY("VALUE_EMPTY"),
    VALUE_TOO_SHORT("VALUE_TOO_SHORT"),
    VALUE_TOO_LONG("VALUE_TOO_LONG"),
    VALUE_TOO_HIGH("VALUE_TOO_HIGH"),
    VALUE_TOO_LOW("VALUE_TOO_LOW"),
    VALUE_INVALID_FORMAT("VALUE_INVALID_FORMAT"),
    VALUE_IS_NOT_EQUAL_TO("VALUE_IS_NOT_EQUAL_TO"),
    ARRAY_LENGTH_TOO_SHORT("ARRAY_LENGTH_TOO_SHORT"),
    ARRAY_LENGTH_TOO_LONG("ARRAY_LENGTH_TOO_LONG"),
    VALUE_INVALID_TYPE("VALUE_INVALID_TYPE"),
    ARRAY_CONTAINS_DUPLICATES("ARRAY_CONTAINS_DUPLICATES"),
    UNEXPECTED_PARAMETER("UNEXPECTED_PARAMETER"),
    CONFLICTING_PARAMETERS("CONFLICTING_PARAMETERS"),
    INVALID_CURSOR("INVALID_CURSOR"),
    INVALID_LIMIT("INVALID_LIMIT"),
    INVALID_PAYLOAD("INVALID_PAYLOAD"),

    BAD_GATEWAY("BAD_GATEWAY"),
    CONFLICT("CONFLICT"),
    MAX_AMOUNT_OF_RESOURCES("MAX_AMOUNT_OF_RESOURCES"),
    INTERNAL_SERVER_ERROR("INTERNAL_SERVER_ERROR"),
    NOT_ACCEPTABLE("NOT_ACCEPTABLE"),
    UNSUPPORTED_MEDIA_TYPE("UNSUPPORTED_MEDIA_TYPE"),
    METHOD_NOT_SUPPORTED("METHOD_NOT_SUPPORTED"),
    NOT_FOUND("NOT_FOUND"),
    SERVICE_UNAVAILABLE("SERVICE_UNAVAILABLE");

    private final String code;

    DefaultErrorCodes(String code) {
        this.code = code;
    }

    @Override
    public String toCode() {
        return code;
    }

}
