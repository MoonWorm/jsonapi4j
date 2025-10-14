package pro.api4.jsonapi4j.http.exception;

import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.exception.JsonApi4jException;

public class MethodNotSupportedException extends JsonApi4jException {

    private String actualMethod;
    private String expectedMethod;

    public MethodNotSupportedException(String actualMethod, String expectedMethod) {
        this("HTTP method is not supported: " + actualMethod + ". Ensure a proper HTTP method for an HTTP request is used: " + expectedMethod);
    }

    public MethodNotSupportedException() {
        this(HttpStatusCodes.SC_405_METHOD_NOT_SUPPORTED.getDescription());
    }

    public MethodNotSupportedException(String message) {
        super(
                HttpStatusCodes.SC_405_METHOD_NOT_SUPPORTED.getCode(),
                DefaultErrorCodes.METHOD_NOT_SUPPORTED,
                message
        );
    }

    public String getActualMethod() {
        return actualMethod;
    }

    public String getExpectedMethod() {
        return expectedMethod;
    }
}
