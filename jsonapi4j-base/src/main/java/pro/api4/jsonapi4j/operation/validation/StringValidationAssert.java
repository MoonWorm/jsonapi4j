package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StringValidationAssert extends ObjectValidationAssert<StringValidationAssert, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    StringValidationAssert(String actual, ErrorSources.Source source) {
        super(actual, source);
    }

    // --- Blank/empty ---

    public StringValidationAssert isBlank() {
        if (StringUtils.isNotBlank(actual)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "value must be blank");
        }
        return this;
    }

    public StringValidationAssert isNotBlank() {
        if (StringUtils.isBlank(actual)) {
            fail(DefaultErrorCodes.VALUE_EMPTY, "value can't be blank");
        }
        return this;
    }

    public StringValidationAssert isEmpty() {
        if (actual != null && !actual.isEmpty()) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "value must be empty");
        }
        return this;
    }

    public StringValidationAssert isNotEmpty() {
        if (actual == null || actual.isEmpty()) {
            fail(DefaultErrorCodes.VALUE_EMPTY, "value can't be empty");
        }
        return this;
    }

    // --- Length ---

    public StringValidationAssert hasLength(int expected) {
        if (actual == null || actual.length() != expected) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("expected length {0}, got {1}", expected, actual == null ? "null" : actual.length()));
        }
        return this;
    }

    public StringValidationAssert hasLengthLessThan(int max) {
        if (actual != null && actual.length() >= max) {
            fail(DefaultErrorCodes.VALUE_TOO_LONG,
                    MessageFormat.format("length must be less than {0}", max));
        }
        return this;
    }

    public StringValidationAssert hasLengthLessThanOrEqualTo(int max) {
        if (actual != null && actual.length() > max) {
            fail(DefaultErrorCodes.VALUE_TOO_LONG,
                    MessageFormat.format("length can''t be more than {0}", max));
        }
        return this;
    }

    public StringValidationAssert hasLengthGreaterThan(int min) {
        if (actual == null || actual.length() <= min) {
            fail(DefaultErrorCodes.VALUE_TOO_SHORT,
                    MessageFormat.format("length must be greater than {0}", min));
        }
        return this;
    }

    public StringValidationAssert hasLengthGreaterThanOrEqualTo(int min) {
        if (actual == null || actual.length() < min) {
            fail(DefaultErrorCodes.VALUE_TOO_SHORT,
                    MessageFormat.format("length can''t be less than {0}", min));
        }
        return this;
    }

    public StringValidationAssert hasLengthBetween(int min, int max) {
        if (actual == null || actual.length() < min || actual.length() > max) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("length must be between {0} and {1}", min, max));
        }
        return this;
    }

    // --- Content ---

    public StringValidationAssert contains(CharSequence s) {
        if (actual == null || !actual.contains(s)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must contain ''{0}''", s));
        }
        return this;
    }

    public StringValidationAssert doesNotContain(CharSequence s) {
        if (actual != null && actual.contains(s)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must not contain ''{0}''", s));
        }
        return this;
    }

    public StringValidationAssert containsIgnoringCase(CharSequence s) {
        if (actual == null || !StringUtils.containsIgnoreCase(actual, s)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must contain ''{0}''", s));
        }
        return this;
    }

    public StringValidationAssert startsWith(String prefix) {
        if (actual == null || !actual.startsWith(prefix)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must start with ''{0}''", prefix));
        }
        return this;
    }

    public StringValidationAssert doesNotStartWith(String prefix) {
        if (actual != null && actual.startsWith(prefix)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must not start with ''{0}''", prefix));
        }
        return this;
    }

    public StringValidationAssert endsWith(String suffix) {
        if (actual == null || !actual.endsWith(suffix)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must end with ''{0}''", suffix));
        }
        return this;
    }

    public StringValidationAssert doesNotEndWith(String suffix) {
        if (actual != null && actual.endsWith(suffix)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must not end with ''{0}''", suffix));
        }
        return this;
    }

    // --- Pattern ---

    public StringValidationAssert matches(String regex) {
        if (actual == null || !actual.matches(regex)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must match pattern ''{0}''", regex));
        }
        return this;
    }

    public StringValidationAssert matches(Pattern pattern) {
        if (actual == null || !pattern.matcher(actual).matches()) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must match pattern ''{0}''", pattern.pattern()));
        }
        return this;
    }

    public StringValidationAssert doesNotMatch(String regex) {
        if (actual != null && actual.matches(regex)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must not match pattern ''{0}''", regex));
        }
        return this;
    }

    // --- Case ---

    public StringValidationAssert isLowerCase() {
        if (actual == null || !actual.equals(actual.toLowerCase())) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be lowercase");
        }
        return this;
    }

    public StringValidationAssert isUpperCase() {
        if (actual == null || !actual.equals(actual.toUpperCase())) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be uppercase");
        }
        return this;
    }

    // --- Format ---

    public StringValidationAssert isEmail() {
        if (actual == null || !EMAIL_PATTERN.matcher(actual).matches()) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "must be a well-formed email address");
        }
        return this;
    }

    public StringValidationAssert isUUID() {
        if (actual == null || !UUID_PATTERN.matcher(actual).matches()) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "must be a valid UUID");
        }
        return this;
    }

    public StringValidationAssert isNumeric() {
        if (actual == null || !StringUtils.isNumeric(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be numeric");
        }
        return this;
    }

    public StringValidationAssert isAlphanumeric() {
        if (actual == null || !StringUtils.isAlphanumeric(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be alphanumeric");
        }
        return this;
    }

    public StringValidationAssert isAlphabetic() {
        if (actual == null || !StringUtils.isAlpha(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be alphabetic");
        }
        return this;
    }

    // --- Enum ---

    public StringValidationAssert isOneOf(String... allowedValues) {
        return isOneOf(Set.of(allowedValues));
    }

    public StringValidationAssert isOneOf(Set<String> allowedValues) {
        for (String allowed : allowedValues) {
            if (allowed.equalsIgnoreCase(actual)) {
                return this;
            }
        }
        fail(DefaultErrorCodes.INVALID_ENUM_VALUE,
                MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]",
                        actual, String.join(", ", allowedValues)));
        return this;
    }

}
