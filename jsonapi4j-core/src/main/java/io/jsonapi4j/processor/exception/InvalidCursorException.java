package io.jsonapi4j.processor.exception;

import lombok.Getter;

/**
 * Can be explicitly thrown from the operation if client sent an invalid cursor value.
 */
@Getter
public class InvalidCursorException extends RuntimeException {

    private final String cursor;

    public InvalidCursorException(String cursor, String message) {
        super(message);
        this.cursor = cursor;
    }

    public InvalidCursorException(String cursor) {
        this(cursor, "Invalid cursor value: " + cursor);
    }

}
