package pro.api4.jsonapi4j.sampleapp.validation;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.List;

@Validated
@Component
@NoArgsConstructor
public class CountryInputParamsValidatorJsr380Impl implements CountryInputParamsValidator {

    @Override
    public void validateCountryId(String countryId) {
        // can be empty
    }

    @Override
    public void validateCountryIds(List<String> countryIds) {
        // can be empty
    }

    @Override
    public void validateRegion(String region) {
        // can be empty
    }

}
