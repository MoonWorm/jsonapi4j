package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.exception.CompositeJsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.ValidationError;

import java.util.ArrayList;
import java.util.List;

public class ValidationErrorCollector {

    private final List<ValidationError> errors = new ArrayList<>();

    /**
     * Collects validation errors from the runnable. The provided source acts as a fallback —
     * it is only used when the exception does not already carry its own source.
     * This allows validators that build their own source (e.g. via {@code field()}) to retain it,
     * while simple validators that throw without a source get the builder-provided one.
     */
    public void collect(Runnable runnable, ErrorSources.Source fallbackSource) {
        try {
            runnable.run();
        } catch (JsonApiRequestValidationException e) {
            ErrorSources.Source effectiveSource = e.getSource() != null ? e.getSource() : fallbackSource;
            errors.add(new ValidationError(e.getErrorCode(), e.getDetail(), effectiveSource));
        } catch (CompositeJsonApiRequestValidationException e) {
            for (ValidationError ve : e.getValidationErrors()) {
                ErrorSources.Source effectiveSource = ve.source() != null ? ve.source() : fallbackSource;
                errors.add(new ValidationError(ve.errorCode(), ve.detail(), effectiveSource));
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
