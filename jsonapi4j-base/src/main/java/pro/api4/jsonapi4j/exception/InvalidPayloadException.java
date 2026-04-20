package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

/**
 * Can be explicitly thrown from the operation when an error happened during converting payload into a valid JSON:API
 * document.
 */
@Getter
public class InvalidPayloadException extends ConstraintViolationException {

    private final Object payload;

    public InvalidPayloadException(String message, Object payload) {
        super(DefaultErrorCodes.INVALID_PAYLOAD, message, "payload");
        this.payload = payload;
    }

    public InvalidPayloadException(String message) {
        this(message, null);
    }

    public InvalidPayloadException() {
        this("Payload seems to be an invalid format. Refer JSON:API spec for more details.");
    }

}
