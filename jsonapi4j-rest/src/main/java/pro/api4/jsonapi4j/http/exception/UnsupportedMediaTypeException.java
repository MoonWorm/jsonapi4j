package pro.api4.jsonapi4j.http.exception;

import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.exception.JsonApi4jException;

public class UnsupportedMediaTypeException extends JsonApi4jException {

    private String actualMediaType;
    private String expectedMediaType;

    public UnsupportedMediaTypeException(String actualMediaType, String expectedMediaType) {
        this("Unsupported Media Type: " + (actualMediaType == null ? "<empty>" : actualMediaType) + ". The JSON:API is using content negotiation. Ensure the proper media type is set into 'Content-Type' header - " + expectedMediaType + ".");
    }

    public UnsupportedMediaTypeException() {
        this(HttpStatusCodes.SC_415_UNSUPPORTED_MEDIA_TYPE.getDescription());
    }

    public UnsupportedMediaTypeException(String message) {
        super(
                HttpStatusCodes.SC_415_UNSUPPORTED_MEDIA_TYPE.getCode(),
                DefaultErrorCodes.UNSUPPORTED_MEDIA_TYPE,
                message
        );
    }

    public String getActualMediaType() {
        return actualMediaType;
    }

    public String getExpectedMediaType() {
        return expectedMediaType;
    }
}
