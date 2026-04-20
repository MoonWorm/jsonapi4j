package pro.api4.jsonapi4j.sampleapp.servlet.validation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserInputParamsValidator;

/**
 * Manual implementation of {@link UserInputParamsValidator} without JSR-380/Hibernate Validator dependency.
 */
public class SimpleUserInputParamsValidator implements UserInputParamsValidator {

    private static final int MAX_NAME_LENGTH = 64;

    @Override
    public void validateFirstName(String firstName) {
        validateNotBlank(firstName, "firstName");
        validateMaxLength(firstName, MAX_NAME_LENGTH, "firstName");
    }

    @Override
    public void validateLastName(String lastName) {
        validateNotBlank(lastName, "lastName");
        validateMaxLength(lastName, MAX_NAME_LENGTH, "lastName");
    }

    @Override
    public void validateEmail(String email) {
        validateNotBlank(email, "email");
        if (!email.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
            throw new ConstraintViolationException(DefaultErrorCodes.GENERIC_REQUEST_ERROR, "Invalid email format", "email");
        }
    }

    private void validateNotBlank(String value, String paramName) {
        if (StringUtils.isBlank(value)) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.VALUE_IS_ABSENT, paramName + " must not be blank", paramName);
        }
    }

    private void validateMaxLength(String value,
                                   int maxLength,
                                   String paramName) {
        if (value != null && value.length() > maxLength) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.VALUE_TOO_LONG,
                    paramName + " must not exceed " + maxLength + " characters",
                    paramName
            );
        }
    }

}
