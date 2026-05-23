package pro.api4.jsonapi4j.exception;

import java.util.List;

public class CompositeJsonApiRequestValidationException extends RuntimeException {

    private final List<ValidationError> validationErrors;

    public CompositeJsonApiRequestValidationException(List<ValidationError> validationErrors) {
        super(validationErrors.size() + " validation errors");
        this.validationErrors = List.copyOf(validationErrors);
    }

    public List<ValidationError> getValidationErrors() {
        return validationErrors;
    }

}
