package pro.api4.jsonapi4j.sampleapp.quarkus.operations.country.validation;

import jakarta.enterprise.context.ApplicationScoped;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.List;

@ApplicationScoped
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
