package pro.api4.jsonapi4j.sampleapp.operations.country.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Component
@NoArgsConstructor
public class CountryInputParamsValidator {

    public void validateCountryId(@Valid @NotBlank @ValidCca2 String countryId) {
    }

    public void validateCountryIds(@Valid @Size(max = 20) List<@NotBlank String> countryIds) {
    }

    public void validateRegion(@Valid @NotBlank @SupportedRegion String region) {
    }

}
