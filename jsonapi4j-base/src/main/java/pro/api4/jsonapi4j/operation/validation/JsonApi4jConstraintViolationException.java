package pro.api4.jsonapi4j.operation.validation;

import lombok.Getter;

@Getter
public class JsonApi4jConstraintViolationException extends RuntimeException {

    private final String detail;
    private final String parameter;

    public JsonApi4jConstraintViolationException(String detail,
                                                 String parameter) {
        super(parameter + ":" + detail);
        this.detail = detail;
        this.parameter = parameter;
    }

}
