package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;

/**
 * Can be explicitly thrown from the operation when an error happened during converting payload into a valid JSON:API
 * document.
 */
@Getter
public class InvalidPayloadException extends JsonApiRequestValidationException {

    private final Object payload;

    public InvalidPayloadException(String detail, Object payload) {
        this(detail, ErrorSources.payload().toParameter(), payload);
    }

    public InvalidPayloadException(String detail, ErrorSources.ParameterPath parameter, Object payload) {
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
