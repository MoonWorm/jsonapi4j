package pro.api4.jsonapi4j.http.exception;

import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.exception.JsonApi4jException;

public class NotAcceptableException extends JsonApi4jException {

    private String actualAccept;
    private String expectedAccept;

    public NotAcceptableException(String actualAccept, String expectedAccept) {
        super(
                HttpStatusCodes.SC_406_NOT_ACCEPTABLE.getCode(),
                DefaultErrorCodes.NOT_ACCEPTABLE,
                "Not acceptable. The server doesn't support any of the requested by client acceptable content types: "
                        + (actualAccept == null ? "<empty>" : actualAccept)
                        + ". Supported content types: "
                        + expectedAccept
        );
        this.actualAccept = actualAccept;
        this.expectedAccept = expectedAccept;
    }

    public NotAcceptableException() {
        this(HttpStatusCodes.SC_406_NOT_ACCEPTABLE.getDescription());
    }

    public NotAcceptableException(String message) {
        super(
                HttpStatusCodes.SC_406_NOT_ACCEPTABLE.getCode(),
                DefaultErrorCodes.NOT_ACCEPTABLE,
                message
        );
    }

    public String getActualAccept() {
        return actualAccept;
    }

    public String getExpectedAccept() {
        return expectedAccept;
    }
}
