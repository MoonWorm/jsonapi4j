package io.jsonapi4j.processor.exception;

/**
 * Can be explicitly thrown from the operation if an error happened during reading downstream data due to some
 * errors on a downstream service side.
 */
public class DataRetrievalException extends RuntimeException {

    public DataRetrievalException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataRetrievalException(String message) {
        super(message);
    }
}
