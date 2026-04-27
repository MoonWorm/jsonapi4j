package pro.api4.jsonapi4j.sampleapp.servlet.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserInputParamsValidator;

import java.lang.reflect.Method;
import java.util.Set;

public class SimpleUserInputParamsValidator implements UserInputParamsValidator {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private static final ExecutableValidator EXEC_VALIDATOR = VALIDATOR.forExecutables();

    private static final Method VALIDATE_FIRST_NAME_METHOD;
    private static final Method VALIDATE_LAST_NAME_METHOD;
    private static final Method VALIDATE_EMAIL_METHOD;

    static {
        try {
            VALIDATE_FIRST_NAME_METHOD = UserInputParamsValidator.class.getMethod("validateFirstName", String.class);
            VALIDATE_LAST_NAME_METHOD = UserInputParamsValidator.class.getMethod("validateLastName", String.class);
            VALIDATE_EMAIL_METHOD = UserInputParamsValidator.class.getMethod("validateEmail", String.class);
        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void validateFirstName(String firstName) {
        validate(VALIDATE_FIRST_NAME_METHOD, firstName);
    }

    @Override
    public void validateLastName(String lastName) {
        validate(VALIDATE_LAST_NAME_METHOD, lastName);
    }

    @Override
    public void validateEmail(String email) {
        validate(VALIDATE_EMAIL_METHOD, email);
    }

    private void validate(Method method, Object arg) {
        Set<ConstraintViolation<Object>> violations =
                EXEC_VALIDATOR.validateParameters(
                        this,
                        method,
                        new Object[]{arg}
                );

        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }
    }

}
