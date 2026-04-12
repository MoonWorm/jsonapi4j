package pro.api4.jsonapi4j.sampleapp.validation;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserInputParamsValidator;

@Validated
@Component
@NoArgsConstructor
public class UserInputParamsValidatorJsr380Impl implements UserInputParamsValidator {

    @Override
    public void validateFirstName(String firstName) {
        // can be empty
    }

    @Override
    public void validateLastName(String lastName) {
        // can be empty
    }

    @Override
    public void validateEmail(String email) {
        // can be empty
    }

}
