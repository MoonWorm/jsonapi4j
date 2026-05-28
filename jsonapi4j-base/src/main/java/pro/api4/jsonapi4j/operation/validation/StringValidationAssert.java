package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Fluent assertion class for validating {@link String} values in JSON:API requests.
 *
 * <p>Extends {@link ObjectValidationAssert} with string-specific constraints: blank/empty checks,
 * length bounds, content matching, regex patterns, case checks, format validations (email, UUID,
 * numeric, alphabetic), and enum membership.
 *
 * <p>Usage example:
 * {@snippet :
 *   Validate.assertThat(name)
 *           .isNotBlank()
 *           .hasLengthBetween(1, 100)
 *           .matches("[a-zA-Z0-9_-]+");
 * }
 *
 * @see Validate
 * @see ObjectValidationAssert
 */
public class StringValidationAssert extends ObjectValidationAssert<StringValidationAssert, String> {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");

    StringValidationAssert(String actual, ErrorSources.Source source) {
        super(actual, source);
    }

    // --- Blank/empty ---

    /** Asserts the value is blank (null, empty, or whitespace only). */
    public StringValidationAssert isBlank() {
        if (isSkipped()) return this;
        if (StringUtils.isNotBlank(actual)) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "value must be blank");
        }
        return this;
    }

    /** Asserts the value is not blank. */
    public StringValidationAssert isNotBlank() {
        if (isSkipped()) return this;
        if (StringUtils.isBlank(actual)) {
            fail(DefaultErrorCodes.VALUE_EMPTY, "value can't be blank");
        }
        return this;
    }

    /** Asserts the value is empty (null or zero-length). */
    public StringValidationAssert isEmpty() {
        if (isSkipped()) return this;
        if (actual != null && !actual.isEmpty()) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "value must be empty");
        }
        return this;
    }

    /** Asserts the value is not empty. */
    public StringValidationAssert isNotEmpty() {
        if (isSkipped()) return this;
        if (actual == null || actual.isEmpty()) {
            fail(DefaultErrorCodes.VALUE_EMPTY, "value can't be empty");
        }
        return this;
    }

    // --- Length ---

    /** Asserts the value has exactly the given length. */
    public StringValidationAssert hasLength(int expected) {
        if (isSkipped()) return this;
        if (actual == null || actual.length() != expected) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("expected length {0}, got {1}", expected, actual == null ? "null" : actual.length()));
        }
        return this;
    }

    /** Asserts the length is strictly less than max. */
    public StringValidationAssert hasLengthLessThan(int max) {
        if (isSkipped()) return this;
        if (actual != null && actual.length() >= max) {
            fail(DefaultErrorCodes.VALUE_TOO_LONG,
                    MessageFormat.format("length must be less than {0}", max));
        }
        return this;
    }

    /** Asserts the length is at most max. */
    public StringValidationAssert hasLengthLessThanOrEqualTo(int max) {
        if (isSkipped()) return this;
        if (actual != null && actual.length() > max) {
            fail(DefaultErrorCodes.VALUE_TOO_LONG,
                    MessageFormat.format("length can''t be more than {0}", max));
        }
        return this;
    }

    /** Asserts the length is strictly greater than min. */
    public StringValidationAssert hasLengthGreaterThan(int min) {
        if (isSkipped()) return this;
        if (actual == null || actual.length() <= min) {
            fail(DefaultErrorCodes.VALUE_TOO_SHORT,
                    MessageFormat.format("length must be greater than {0}", min));
        }
        return this;
    }

    /** Asserts the length is at least min. */
    public StringValidationAssert hasLengthGreaterThanOrEqualTo(int min) {
        if (isSkipped()) return this;
        if (actual == null || actual.length() < min) {
            fail(DefaultErrorCodes.VALUE_TOO_SHORT,
                    MessageFormat.format("length can''t be less than {0}", min));
        }
        return this;
    }

    /** Asserts the length is between min and max (inclusive). */
    public StringValidationAssert hasLengthBetween(int min, int max) {
        if (isSkipped()) return this;
        if (actual == null || actual.length() < min || actual.length() > max) {
            fail(DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    MessageFormat.format("length must be between {0} and {1}", min, max));
        }
        return this;
    }

    // --- Content ---

    /** Asserts the value contains the given subsequence. */
    public StringValidationAssert contains(CharSequence s) {
        if (isSkipped()) return this;
        if (actual == null || !actual.contains(s)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must contain ''{0}''", s));
        }
        return this;
    }

    /** Asserts the value does not contain the given subsequence. */
    public StringValidationAssert doesNotContain(CharSequence s) {
        if (isSkipped()) return this;
        if (actual != null && actual.contains(s)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must not contain ''{0}''", s));
        }
        return this;
    }

    /** Asserts the value contains the subsequence, ignoring case. */
    public StringValidationAssert containsIgnoringCase(CharSequence s) {
        if (isSkipped()) return this;
        if (actual == null || !Strings.CI.contains(actual, s)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must contain ''{0}''", s));
        }
        return this;
    }

    /** Asserts the value starts with the given prefix. */
    public StringValidationAssert startsWith(String prefix) {
        if (isSkipped()) return this;
        if (actual == null || !actual.startsWith(prefix)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must start with ''{0}''", prefix));
        }
        return this;
    }

    /** Asserts the value does not start with the given prefix. */
    public StringValidationAssert doesNotStartWith(String prefix) {
        if (isSkipped()) return this;
        if (actual != null && actual.startsWith(prefix)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must not start with ''{0}''", prefix));
        }
        return this;
    }

    /** Asserts the value ends with the given suffix. */
    public StringValidationAssert endsWith(String suffix) {
        if (isSkipped()) return this;
        if (actual == null || !actual.endsWith(suffix)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must end with ''{0}''", suffix));
        }
        return this;
    }

    /** Asserts the value does not end with the given suffix. */
    public StringValidationAssert doesNotEndWith(String suffix) {
        if (isSkipped()) return this;
        if (actual != null && actual.endsWith(suffix)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must not end with ''{0}''", suffix));
        }
        return this;
    }

    // --- Pattern ---

    /** Asserts the value matches the given regex. */
    public StringValidationAssert matches(String regex) {
        if (isSkipped()) return this;
        if (actual == null || !actual.matches(regex)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must match pattern ''{0}''", regex));
        }
        return this;
    }

    /** Asserts the value matches the given compiled pattern. */
    public StringValidationAssert matches(Pattern pattern) {
        if (isSkipped()) return this;
        if (actual == null || !pattern.matcher(actual).matches()) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must match pattern ''{0}''", pattern.pattern()));
        }
        return this;
    }

    /** Asserts the value does not match the given regex. */
    public StringValidationAssert doesNotMatch(String regex) {
        if (isSkipped()) return this;
        if (actual != null && actual.matches(regex)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    MessageFormat.format("value must not match pattern ''{0}''", regex));
        }
        return this;
    }

    // --- Case ---

    /** Asserts the value is all lowercase. */
    public StringValidationAssert isLowerCase() {
        if (isSkipped()) return this;
        if (actual == null || !actual.equals(actual.toLowerCase())) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be lowercase");
        }
        return this;
    }

    /** Asserts the value is all uppercase. */
    public StringValidationAssert isUpperCase() {
        if (isSkipped()) return this;
        if (actual == null || !actual.equals(actual.toUpperCase())) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be uppercase");
        }
        return this;
    }

    // --- Format ---

    /** Asserts the value is a well-formed email address. */
    public StringValidationAssert isEmail() {
        if (isSkipped()) return this;
        if (actual == null || !EMAIL_PATTERN.matcher(actual).matches()) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "must be a well-formed email address");
        }
        return this;
    }

    /** Asserts the value is a valid UUID. */
    public StringValidationAssert isUUID() {
        if (isSkipped()) return this;
        if (actual == null || !UUID_PATTERN.matcher(actual).matches()) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "must be a valid UUID");
        }
        return this;
    }

    /** Asserts the value contains only digits. */
    public StringValidationAssert isNumeric() {
        if (isSkipped()) return this;
        if (!StringUtils.isNumeric(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be numeric");
        }
        return this;
    }

    /** Asserts the value contains only letters and digits. */
    public StringValidationAssert isAlphanumeric() {
        if (isSkipped()) return this;
        if (!StringUtils.isAlphanumeric(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be alphanumeric");
        }
        return this;
    }

    /** Asserts the value contains only letters. */
    public StringValidationAssert isAlphabetic() {
        if (isSkipped()) return this;
        if (!StringUtils.isAlpha(actual)) {
            fail(DefaultErrorCodes.VALUE_INVALID_FORMAT, "value must be alphabetic");
        }
        return this;
    }

    // --- Enum ---

    /** Asserts the value is one of the allowed values (case-insensitive). */
    public StringValidationAssert isOneOf(String... allowedValues) {
        if (isSkipped()) return this;
        return isOneOf(Set.of(allowedValues));
    }

    /** Asserts the value is one of the allowed values (case-insensitive). */
    public StringValidationAssert isOneOf(Set<String> allowedValues) {
        if (isSkipped()) return this;
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
