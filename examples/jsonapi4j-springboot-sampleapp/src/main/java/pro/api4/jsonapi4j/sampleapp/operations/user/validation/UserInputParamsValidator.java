package pro.api4.jsonapi4j.sampleapp.operations.user.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.NoArgsConstructor;
import org.hibernate.validator.constraints.Length;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Validated
@Component
@NoArgsConstructor
public class UserInputParamsValidator {

    public void validateFirstName(@Valid @NotBlank @Length(min = 1, max = 64) String firstName) {
    }

    public void validateLastName(@Valid @NotBlank @Length(min = 1, max = 64) String lastName) {
    }

    public void validateEmail(@Valid @NotBlank @Email String email) {
    }

}
