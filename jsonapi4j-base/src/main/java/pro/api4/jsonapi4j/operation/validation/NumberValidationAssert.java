package pro.api4.jsonapi4j.operation.validation;

import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;

public class NumberValidationAssert<T extends Number & Comparable<T>>
        extends ObjectValidationAssert<NumberValidationAssert<T>, T> {

    NumberValidationAssert(T actual, ErrorSources.Source source) {
        super(actual, source);
    }

    public NumberValidationAssert<T> isZero() {
        if (actual == null || actual.doubleValue() != 0) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "value must be zero");
        }
        return this;
    }

    public NumberValidationAssert<T> isNotZero() {
        if (actual != null && actual.doubleValue() == 0) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "value must not be zero");
        }
        return this;
    }

    public NumberValidationAssert<T> isPositive() {
        if (actual == null || actual.doubleValue() <= 0) {
            fail(DefaultErrorCodes.VALUE_TOO_LOW, "value must be positive");
        }
        return this;
    }

    public NumberValidationAssert<T> isNegative() {
        if (actual == null || actual.doubleValue() >= 0) {
            fail(DefaultErrorCodes.VALUE_TOO_HIGH, "value must be negative");
        }
        return this;
    }

    public NumberValidationAssert<T> isNotNegative() {
        if (actual != null && actual.doubleValue() < 0) {
            fail(DefaultErrorCodes.VALUE_TOO_LOW, "value must not be negative");
        }
        return this;
    }

    public NumberValidationAssert<T> isNotPositive() {
        if (actual != null && actual.doubleValue() > 0) {
            fail(DefaultErrorCodes.VALUE_TOO_HIGH, "value must not be positive");
        }
        return this;
    }

    public NumberValidationAssert<T> isGreaterThan(T value) {
        if (actual == null || actual.compareTo(value) <= 0) {
            fail(DefaultErrorCodes.VALUE_TOO_LOW,
                    MessageFormat.format("value must be greater than {0}", value));
        }
        return this;
    }

    public NumberValidationAssert<T> isGreaterThanOrEqualTo(T value) {
        if (actual == null || actual.compareTo(value) < 0) {
            fail(DefaultErrorCodes.VALUE_TOO_LOW,
                    MessageFormat.format("value must be greater than or equal to {0}", value));
        }
        return this;
    }

    public NumberValidationAssert<T> isLessThan(T value) {
        if (actual == null || actual.compareTo(value) >= 0) {
            fail(DefaultErrorCodes.VALUE_TOO_HIGH,
                    MessageFormat.format("value must be less than {0}", value));
        }
        return this;
    }

    public NumberValidationAssert<T> isLessThanOrEqualTo(T value) {
        if (actual == null || actual.compareTo(value) > 0) {
            fail(DefaultErrorCodes.VALUE_TOO_HIGH,
                    MessageFormat.format("value must be less than or equal to {0}", value));
        }
        return this;
    }

    public NumberValidationAssert<T> isBetween(T start, T end) {
        if (actual == null || actual.compareTo(start) < 0 || actual.compareTo(end) > 0) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("value must be between {0} and {1}", start, end));
        }
        return this;
    }

    public NumberValidationAssert<T> isStrictlyBetween(T start, T end) {
        if (actual == null || actual.compareTo(start) <= 0 || actual.compareTo(end) >= 0) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("value must be strictly between {0} and {1}", start, end));
        }
        return this;
    }

}
