package pro.api4.jsonapi4j.sampleapp.operations.country.validation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public interface CountryInputParamsValidator {

    void validateCountryId(@Valid @NotBlank @ValidCca2 String countryId);

    void validateCountryIds(@Valid @Size(max = 20) List<@NotBlank String> countryIds);

    void validateRegion(@Valid @NotBlank @SupportedRegion String region);

}
