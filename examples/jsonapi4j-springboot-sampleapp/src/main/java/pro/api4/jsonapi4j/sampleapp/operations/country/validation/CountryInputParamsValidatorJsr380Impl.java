package pro.api4.jsonapi4j.sampleapp.operations.country.validation;

import lombok.NoArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.util.List;

@Validated
@Component
@NoArgsConstructor
public class CountryInputParamsValidatorJsr380Impl implements CountryInputParamsValidator {

    @Override
    public void validateCountryId(String countryId) {
    }

    @Override
    public void validateCountryIds(List<String> countryIds) {
    }

    @Override
    public void validateRegion(String region) {
    }

}
