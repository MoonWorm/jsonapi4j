package pro.api4.jsonapi4j.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;

@Getter
public class ConstraintViolationException extends JsonApi4jException {

    private final String detail;
    private final String parameter;

    public ConstraintViolationException(ErrorCode errorCode,
                                        String detail,
                                        String parameter) {
        super(HttpStatusCodes.SC_400_BAD_REQUEST.getCode(), errorCode, parameter + ":" + detail);
        this.detail = detail;
        this.parameter = parameter;
    }

    public ConstraintViolationException(String detail,
                                        String parameter) {
        this(DefaultErrorCodes.GENERIC_REQUEST_ERROR, detail, parameter);
    }

}
