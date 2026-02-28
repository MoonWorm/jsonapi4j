package pro.api4.jsonapi4j.sampleapp.quarkus.operations.country.validation;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import lombok.NoArgsConstructor;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.List;

@ApplicationScoped
@NoArgsConstructor
public class CountryInputParamsValidatorJsr380Impl implements CountryInputParamsValidator {

    @Inject
    Validator validator;

    @Override
    public void validateCountryId(String countryId) {
        var violations = validator.validateValue(
                CountryInputParamsValidatorJsr380Impl.class,
                "countryId",
                countryId
        );
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @Override
    public void validateCountryIds(List<String> countryIds) {
        var violations = validator.validateValue(
                CountryInputParamsValidatorJsr380Impl.class,
                "countryIds",
                countryIds
        );
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    @Override
    public void validateRegion(String region) {
        var violations = validator.validateValue(
                CountryInputParamsValidatorJsr380Impl.class,
                "region",
                region
        );
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

}
