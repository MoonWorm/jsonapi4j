package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;

import java.text.MessageFormat;
import java.util.Set;

public final class ValidationAssertions {

    private ValidationAssertions() {

    }

    public static void validateNonNull(Object object, ErrorSources.Source source) {
        if (object == null) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.VALUE_IS_ABSENT,
                    "value can't be null",
                    source
            );
        }
    }

    public static void validateNonNull(Object object) {
        validateNonNull(object, null);
    }

    public static void validateEqualTo(Object actual, Object expected, ErrorSources.Source source) {
        if (!actual.equals(expected)) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.VALUE_IS_NOT_EQUAL_TO,
                    MessageFormat.format("value should match {0}", expected),
                    source
            );
        }
    }

    public static void validateEqualTo(Object actual, Object expected) {
        validateEqualTo(actual, expected, null);
    }

    public static void validateNonBlank(String value, ErrorSources.Source source) {
        if (StringUtils.isBlank(value)) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.VALUE_EMPTY,
                    "value can't be blank",
                    source
            );
        }
    }

    public static void validateNonBlank(String value) {
        validateNonBlank(value, null);
    }

    public static void validateIsNull(Object value, ErrorSources.Source source) {
        if (value != null) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.VALUE_IS_NOT_ABSENT,
                    "value must be null",
                    source
            );
        }
    }

    public static void validateIsNull(Object value) {
        validateIsNull(value, null);
    }

    public static void validateValueAnyOf(String value,
                                          Set<String> allowedValues,
                                          ErrorSources.Source source) {
        for (String allowedValue : allowedValues) {
            if (allowedValue.equalsIgnoreCase(value)) {
                return;
            }
        }
        throw new JsonApiRequestValidationException(
                DefaultErrorCodes.INVALID_ENUM_VALUE,
                MessageFormat.format("''{0}'' value is not allowed, available values: [{1}]", value, String.join(", ", allowedValues)),
                source
        );
    }

    public static void validateValueAnyOf(String value,
                                          Set<String> allowedValues) {
        validateValueAnyOf(value, allowedValues, null);
    }

}
