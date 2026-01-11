package pro.api4.jsonapi4j.sampleapp.operations.country.validation;

import pro.api4.jsonapi4j.sampleapp.domain.country.Region;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class SupportedRegionValidator implements ConstraintValidator<SupportedRegion, String> {

    @Override
    public boolean isValid(String regionStr, ConstraintValidatorContext constraintValidatorContext) {
        if (regionStr == null) {
            return true;
        }
        return Region.fromName(regionStr) != null;
    }

}
