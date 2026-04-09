package pro.api4.jsonapi4j.sampleapp.quarkus.operations.user.validation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.executable.ValidateOnExecution;
import lombok.NoArgsConstructor;
import pro.api4.jsonapi4j.sampleapp.operations.user.UserInputParamsValidator;

@ApplicationScoped
@NoArgsConstructor
public class UserInputParamsValidatorJsr380Impl implements UserInputParamsValidator {

    @ValidateOnExecution
    @Override
    public void validateFirstName(String firstName) {
        // can be empty
    }

    @ValidateOnExecution
    @Override
    public void validateLastName(String lastName) {
        // can be empty
    }

    @ValidateOnExecution
    @Override
    public void validateEmail(String email) {
        // can be empty
    }

}
