package pro.api4.jsonapi4j.exception;

import java.util.List;

/**
 * Exception that aggregates multiple validation errors from a single request into one throw.
 * <p>
 * Used when an operation's validation logic discovers more than one error and wants to report
 * all of them to the client in a single JSON:API {@code errors} array rather than stopping
 * at the first failure. The framework's error handler converts the contained
 * {@link ValidationError} list into individual JSON:API error objects.
 *
 * @see JsonApiRequestValidationException
 * @see ValidationError
 */
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
