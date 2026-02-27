package pro.api4.jsonapi4j.request.exception;

import lombok.Getter;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.error.ErrorCode;

@Getter
public class ForbiddenJsonApiRequestException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String parameter;

    public ForbiddenJsonApiRequestException(String parameter,
                                            String message) {
        this(DefaultErrorCodes.FORBIDDEN, parameter, message);
    }

    public ForbiddenJsonApiRequestException(ErrorCode errorCode,
                                            String parameter,
                                            String message) {
        super(message);
        this.errorCode = errorCode;
        this.parameter = parameter;
    }

}
