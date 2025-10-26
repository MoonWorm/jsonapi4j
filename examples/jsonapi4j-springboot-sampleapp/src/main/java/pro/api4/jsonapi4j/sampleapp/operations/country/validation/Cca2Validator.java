package pro.api4.jsonapi4j.sampleapp.operations.country.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.Locale;

public class Cca2Validator implements ConstraintValidator<ValidCca2, String> {

    @Override
    public boolean isValid(String cca2,
                           ConstraintValidatorContext constraintValidatorContext) {

        if (cca2 == null) {
            return true;
        }

        if (cca2.length() != 2) {
            return false;
        }

        for (String iso : Locale.getISOCountries()) {
            if (iso.equals(cca2.toUpperCase())) {
                return true;
            }
        }

        return false;
    }

}
