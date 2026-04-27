package pro.api4.jsonapi4j.sampleapp.servlet.validation;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.executable.ExecutableValidator;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Set;

public class SimpleCountryInputParamsValidator implements CountryInputParamsValidator {

    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();
    private static final ExecutableValidator EXEC_VALIDATOR = VALIDATOR.forExecutables();

    private static final Method VALIDATE_COUNTRY_ID_METHOD;
    private static final Method VALIDATE_COUNTRY_IDS_METHOD;
    private static final Method VALIDATE_REGION_METHOD;

    static {
        try {
            VALIDATE_COUNTRY_ID_METHOD =
                    CountryInputParamsValidator.class
                            .getMethod("validateCountryId", String.class);

            VALIDATE_COUNTRY_IDS_METHOD =
                    CountryInputParamsValidator.class
                            .getMethod("validateCountryIds", List.class);

            VALIDATE_REGION_METHOD =
                    CountryInputParamsValidator.class
                            .getMethod("validateRegion", String.class);

        } catch (NoSuchMethodException e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    @Override
    public void validateCountryId(String countryId) {
        validate(VALIDATE_COUNTRY_ID_METHOD, countryId);
    }

    @Override
    public void validateCountryIds(List<String> countryIds) {
        validate(VALIDATE_COUNTRY_IDS_METHOD, countryIds);
    }

    @Override
    public void validateRegion(String region) {
        validate(VALIDATE_REGION_METHOD, region);
    }

    private void validate(Method method, Object arg) {
        Set<ConstraintViolation<Object>> violations =
                EXEC_VALIDATOR.validateParameters(
                        this,
                        method,
                        new Object[]{arg}
                );

        if (!violations.isEmpty()) {
            throw new jakarta.validation.ConstraintViolationException(violations);
        }
    }

}
