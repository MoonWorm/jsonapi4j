package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * Signals that the request body could not be parsed or mapped to a valid JSON:API document (HTTP 400).
 * <p>
 * Thrown automatically by the framework when payload deserialization fails, or can be thrown
 * explicitly from an operation when business-level payload validation fails. Carries the raw
 * payload object that triggered the error (may be {@code null}).
 */
@Getter
public class InvalidPayloadException extends JsonApiRequestValidationException {

    private final Object payload;

    public InvalidPayloadException(String detail, Object payload) {
        this(detail, ErrorSources.pointer().toPointer(), payload);
    }

    public InvalidPayloadException(String detail, ErrorSources.JsonPointer parameter, Object payload) {
        super(DefaultErrorCodes.INVALID_PAYLOAD, detail, parameter);
        this.payload = payload;
    }

    public InvalidPayloadException(String message) {
        this(message, null);
    }

    public InvalidPayloadException() {
        this("Payload seems to be an invalid format. Refer JSON:API spec for more details.");
    }

}
