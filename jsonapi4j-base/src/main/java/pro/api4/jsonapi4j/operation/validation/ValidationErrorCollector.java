package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.exception.CompositeJsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class ValidationErrorCollector {

    private final List<ValidationError> errors = new ArrayList<>();

    public void collect(Runnable runnable, ErrorSources.Source source) {
        try {
            runnable.run();
        } catch (JsonApiRequestValidationException e) {
            var wrapped = JsonApiRequestValidationException.withSource(e, source);
            errors.add(new ValidationError(wrapped.getErrorCode(), wrapped.getDetail(), wrapped.getSource()));
        } catch (CompositeJsonApiRequestValidationException e) {
            for (ValidationError ve : e.getValidationErrors()) {
                errors.add(new ValidationError(ve.errorCode(), ve.detail(), source));
            }
        }
    }

    public void collect(Runnable runnable) {
        try {
            runnable.run();
        } catch (JsonApiRequestValidationException e) {
            errors.add(new ValidationError(e.getErrorCode(), e.getDetail(), e.getSource()));
        } catch (CompositeJsonApiRequestValidationException e) {
            errors.addAll(e.getValidationErrors());
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    public void throwIfErrors() {
        if (errors.size() == 1) {
            ValidationError error = errors.getFirst();
            throw new JsonApiRequestValidationException(error.errorCode(), error.detail(), error.source());
        } else if (errors.size() > 1) {
            throw new CompositeJsonApiRequestValidationException(List.copyOf(errors));
        }
    }

}
