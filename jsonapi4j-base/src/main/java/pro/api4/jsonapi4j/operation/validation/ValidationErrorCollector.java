package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.http.HttpStatusCodes;

import pro.api4.jsonapi4j.exception.CompositeJsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.ValidationError;

import java.util.ArrayList;
import java.util.List;

/**
 * Collects validation errors from multiple validation assertions and throws them as a single
 * composite exception.
 *
 * <p>Instead of failing on the first validation error, this collector runs all validations and
 * accumulates their errors. After all validations have been executed, {@link #throwIfErrors()}
 * throws a single {@link pro.api4.jsonapi4j.exception.JsonApiRequestValidationException} (for one error)
 * or a {@link pro.api4.jsonapi4j.exception.CompositeJsonApiRequestValidationException} (for multiple errors),
 * enabling clients to receive all validation problems in one response.
 *
 * @see JsonApiRequestValidator
 * @see ErrorSources
 */
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
            rethrowIfItCarriesItsOwnStatus(e);
            ErrorSources.Source effectiveSource = e.getSource() != null ? e.getSource() : fallbackSource;
            errors.add(new ValidationError(e.getErrorCode(), e.getDetail(), effectiveSource));
        } catch (CompositeJsonApiRequestValidationException e) {
            for (ValidationError ve : e.getValidationErrors()) {
                ErrorSources.Source effectiveSource = ve.source() != null ? ve.source() : fallbackSource;
                errors.add(new ValidationError(ve.errorCode(), ve.detail(), effectiveSource));
            }
        }
    }

    /**
     * Collects validation errors from the runnable, preserving the exception's original source.
     */
    public void collect(Runnable runnable) {
        try {
            runnable.run();
        } catch (JsonApiRequestValidationException e) {
            rethrowIfItCarriesItsOwnStatus(e);
            errors.add(new ValidationError(e.getErrorCode(), e.getDetail(), e.getSource()));
        } catch (CompositeJsonApiRequestValidationException e) {
            errors.addAll(e.getValidationErrors());
        }
    }

    /**
     * Returns whether any validation errors have been collected.
     */
    /**
     * Lets a failure that names its own HTTP status through untouched.
     *
     * <p>Collecting exists to report every problem with a request at once, which works while they share a
     * status — a JSON:API document carries many errors but one status code. A failure requiring
     * {@code 403} or {@code 409} cannot join that: merging it would answer with {@code 400} and break the
     * rule that asked for it. So it is reported alone, and whatever else was collected is dropped.
     */
    private void rethrowIfItCarriesItsOwnStatus(JsonApiRequestValidationException e) {
        if (e.getHttpStatus() != HttpStatusCodes.SC_400_BAD_REQUEST.getCode()) {
            throw e;
        }
    }

    public boolean hasErrors() {
        return !errors.isEmpty();
    }

    /**
     * Throws collected errors as a single exception, or
     * {@link CompositeJsonApiRequestValidationException} for multiple.
     */
    public void throwIfErrors() {
        if (errors.size() == 1) {
            ValidationError error = errors.get(0);
            throw new JsonApiRequestValidationException(error.errorCode(), error.detail(), error.source());
        } else if (errors.size() > 1) {
            throw new CompositeJsonApiRequestValidationException(List.copyOf(errors));
        }
    }

}
