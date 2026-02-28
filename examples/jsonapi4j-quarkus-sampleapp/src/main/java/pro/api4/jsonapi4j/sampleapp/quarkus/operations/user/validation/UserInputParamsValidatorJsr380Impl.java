package pro.api4.jsonapi4j.sampleapp.quarkus.operations.user.validation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.NoArgsConstructor;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserInputParamsValidator;
import pro.api4.jsonapi4j.sampleapp.quarkus.operations.country.validation.CountryInputParamsValidatorJsr380Impl;

@ApplicationScoped
@NoArgsConstructor
public class UserInputParamsValidatorJsr380Impl implements UserInputParamsValidator {

    @Inject
    Validator validator;

    @Override
    public void validateFirstName(String firstName) {
        var violations = validator.validateValue(
                CountryInputParamsValidatorJsr380Impl.class,
                "firstName",
                firstName
        );
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @Override
    public void validateLastName(String lastName) {
        var violations = validator.validateValue(
                CountryInputParamsValidatorJsr380Impl.class,
                "lastName",
                lastName
        );
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @Override
    public void validateEmail(String email) {
        var violations = validator.validateValue(
                CountryInputParamsValidatorJsr380Impl.class,
                "email",
                email
        );
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

}
