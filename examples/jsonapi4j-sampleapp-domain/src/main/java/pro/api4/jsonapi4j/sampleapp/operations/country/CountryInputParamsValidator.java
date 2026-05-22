package pro.api4.jsonapi4j.sampleapp.operations.country;

import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;


public final class CountryInputParamsValidator {

    private CountryInputParamsValidator() {

    }

    private static final Set<String> VALID_CCA2_CODES = Arrays.stream(Locale.getISOCountries())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

    public static void validateCountryId(String countryId) {
        if (countryId == null) {
            return;
        }
        if (!VALID_CCA2_CODES.contains(countryId.toUpperCase())) {
            throw new JsonApiRequestValidationException(DefaultErrorCodes.VALUE_INVALID_FORMAT, "Not valid CCA2 country code");
        }
    }

    public static void validateCountryIds(List<String> countryIds) {
        if (countryIds == null) {
            return;
        }
        for (String countryId : countryIds) {
            validateCountryId(countryId);
        }
    }

    public static void validateRegion(String region) {
        if (region == null) {
            return;
        }
        if (Region.fromName(region) == null) {
            throw new JsonApiRequestValidationException(DefaultErrorCodes.VALUE_INVALID_FORMAT, "Unknown region");
        }
    }

}
