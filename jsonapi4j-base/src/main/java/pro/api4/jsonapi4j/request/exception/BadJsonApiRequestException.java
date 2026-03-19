package pro.api4.jsonapi4j.request.exception;

import pro.api4.jsonapi4j.model.document.error.ErrorCode;
import lombok.Getter;

@Getter
public class BadJsonApiRequestException extends RuntimeException{

    private final ErrorCode errorCode;
    private final String parameter;

    public BadJsonApiRequestException(ErrorCode errorCode,
                                      String parameter,
                                      String message) {
        super(message);
        this.errorCode = errorCode;
        this.parameter = parameter;
    }

}
