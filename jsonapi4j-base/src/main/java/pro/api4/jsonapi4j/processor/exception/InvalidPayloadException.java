package pro.api4.jsonapi4j.processor.exception;

import lombok.Getter;

/**
 * Can be explicitly thrown from the operation when an error happened during converting payload into a valid JSON:API
 * document.
 */
@Getter
public class InvalidPayloadException extends RuntimeException {

    private final Object payload;

    public InvalidPayloadException(String message, Object payload) {
        super(message);
        this.payload = payload;
    }

    public InvalidPayloadException(String message) {
        this(message, null);
    }

    public InvalidPayloadException() {
        this("Couldn't deserialize payload into a valid JSON:API document. Refer JSON:API spec for more details.");
    }

}
