package pro.api4.jsonapi4j.sampleapp.operations.user;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public interface UserInputParamsValidator {

    void validateFirstName(@Valid @NotBlank @Size(min = 1, max = 64) String firstName);

    void validateLastName(@Valid @NotBlank @Size(min = 1, max = 64) String lastName);

    void validateEmail(@Valid @NotBlank @Email String email);

}
