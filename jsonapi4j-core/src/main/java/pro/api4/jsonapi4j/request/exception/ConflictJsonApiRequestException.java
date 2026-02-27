package pro.api4.jsonapi4j.request.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;

@Getter
public class ConflictJsonApiRequestException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String parameter;

    public ConflictJsonApiRequestException(String parameter,
                                           String message) {
        this(DefaultErrorCodes.CONFLICT, parameter, message);
    }

    public ConflictJsonApiRequestException(ErrorCode errorCode,
                                           String parameter,
                                           String message) {
        super(message);
        this.errorCode = errorCode;
        this.parameter = parameter;
    }

}
