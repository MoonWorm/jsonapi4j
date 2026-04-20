package pro.api4.jsonapi4j.sampleapp.servlet.validation;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Manual implementation of {@link CountryInputParamsValidator} without JSR-380/Hibernate Validator dependency.
 */
public class SimpleCountryInputParamsValidator implements CountryInputParamsValidator {

    private static final Set<String> ISO_COUNTRIES = Set.of(Locale.getISOCountries());
    private static final int MAX_COUNTRY_IDS = 20;

    @Override
    public void validateCountryId(String countryId) {
        validateNotBlank(countryId, "countryId");
        if (!ISO_COUNTRIES.contains(countryId.toUpperCase())) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.VALUE_INVALID_FORMAT,
                    "Invalid ISO 3166 country code: " + countryId, "countryId");
        }
    }

    @Override
    public void validateCountryIds(List<String> countryIds) {
        if (countryIds != null) {
            if (countryIds.size() > MAX_COUNTRY_IDS) {
                throw new ConstraintViolationException(
                        DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                        "Country IDs list must not exceed " + MAX_COUNTRY_IDS + " items", "countryIds");
            }
            ListUtils.emptyIfNull(countryIds).forEach(id -> validateNotBlank(id, "countryIds"));
        }
    }

    @Override
    public void validateRegion(String region) {
        validateNotBlank(region, "region");
        if (Region.fromName(region) == null) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.GENERIC_REQUEST_ERROR,
                    "Unknown region", "region");
        }
    }

    private void validateNotBlank(String value, String paramName) {
        if (StringUtils.isBlank(value)) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.VALUE_IS_ABSENT, paramName + " must not be blank", paramName);
        }
    }

}
