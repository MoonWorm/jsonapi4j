package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;

/**
 * Fluent assertion class for validating numeric values in JSON:API requests.
 *
 * <p>Extends {@link ObjectValidationAssert} with number-specific constraints: sign checks
 * (positive, negative, zero), comparisons (greater than, less than), and range validations
 * (between, strictly between).
 *
 * <p>Usage example:
 * {@snippet :
 *   Validate.assertThat(request.getLimit())
 *           .isNotNull()
 *           .isPositive()
 *           .isLessThanOrEqualTo(100L);
 * }
 *
 * @param <T> the concrete number type (e.g., {@link Integer}, {@link Long})
 * @see Validate
 * @see ObjectValidationAssert
 */
public class NumberValidationAssert<T extends Number & Comparable<T>>
        extends ObjectValidationAssert<NumberValidationAssert<T>, T> {

    NumberValidationAssert(T actual, ErrorSources.Source source) {
        super(actual, source);
    }

    /** Asserts the value is zero. */
    public NumberValidationAssert<T> isZero() {
        if (isSkipped()) return this;
        if (actual == null || actual.doubleValue() != 0) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "value must be zero");
        }
        return this;
    }

    /** Asserts the value is not zero. */
    public NumberValidationAssert<T> isNotZero() {
        if (isSkipped()) return this;
        if (actual != null && actual.doubleValue() == 0) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "value must not be zero");
        }
        return this;
    }

    /** Asserts the value is strictly positive. */
    public NumberValidationAssert<T> isPositive() {
        if (isSkipped()) return this;
        if (actual == null || actual.doubleValue() <= 0) {
            fail(DefaultErrorCodes.VALUE_TOO_LOW, "value must be positive");
        }
        return this;
    }

    /** Asserts the value is strictly negative. */
    public NumberValidationAssert<T> isNegative() {
        if (isSkipped()) return this;
        if (actual == null || actual.doubleValue() >= 0) {
            fail(DefaultErrorCodes.VALUE_TOO_HIGH, "value must be negative");
        }
        return this;
    }

    /** Asserts the value is zero or positive. */
    public NumberValidationAssert<T> isNotNegative() {
        if (isSkipped()) return this;
        if (actual != null && actual.doubleValue() < 0) {
            fail(DefaultErrorCodes.VALUE_TOO_LOW, "value must not be negative");
        }
        return this;
    }

    /** Asserts the value is zero or negative. */
    public NumberValidationAssert<T> isNotPositive() {
        if (isSkipped()) return this;
        if (actual != null && actual.doubleValue() > 0) {
            fail(DefaultErrorCodes.VALUE_TOO_HIGH, "value must not be positive");
        }
        return this;
    }

    /** Asserts the value is strictly greater than the given bound. */
    public NumberValidationAssert<T> isGreaterThan(T value) {
        if (isSkipped()) return this;
        if (actual == null || actual.compareTo(value) <= 0) {
            fail(DefaultErrorCodes.VALUE_TOO_LOW,
                    MessageFormat.format("value must be greater than {0}", value));
        }
        return this;
    }

    /** Asserts the value is greater than or equal to the given bound. */
    public NumberValidationAssert<T> isGreaterThanOrEqualTo(T value) {
        if (isSkipped()) return this;
        if (actual == null || actual.compareTo(value) < 0) {
            fail(DefaultErrorCodes.VALUE_TOO_LOW,
                    MessageFormat.format("value must be greater than or equal to {0}", value));
        }
        return this;
    }

    /** Asserts the value is strictly less than the given bound. */
    public NumberValidationAssert<T> isLessThan(T value) {
        if (isSkipped()) return this;
        if (actual == null || actual.compareTo(value) >= 0) {
            fail(DefaultErrorCodes.VALUE_TOO_HIGH,
                    MessageFormat.format("value must be less than {0}", value));
        }
        return this;
    }

    /** Asserts the value is less than or equal to the given bound. */
    public NumberValidationAssert<T> isLessThanOrEqualTo(T value) {
        if (isSkipped()) return this;
        if (actual == null || actual.compareTo(value) > 0) {
            fail(DefaultErrorCodes.VALUE_TOO_HIGH,
                    MessageFormat.format("value must be less than or equal to {0}", value));
        }
        return this;
    }

    /** Asserts the value is between start and end (inclusive). */
    public NumberValidationAssert<T> isBetween(T start, T end) {
        if (isSkipped()) return this;
        if (actual == null || actual.compareTo(start) < 0 || actual.compareTo(end) > 0) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("value must be between {0} and {1}", start, end));
        }
        return this;
    }

    /** Asserts the value is strictly between start and end (exclusive). */
    public NumberValidationAssert<T> isStrictlyBetween(T start, T end) {
        if (isSkipped()) return this;
        if (actual == null || actual.compareTo(start) <= 0 || actual.compareTo(end) >= 0) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("value must be strictly between {0} and {1}", start, end));
        }
        return this;
    }

}
