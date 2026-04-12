package pro.api4.jsonapi4j.sampleapp.servlet.validation;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.request.exception.BadJsonApiRequestException;
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
            throw new BadJsonApiRequestException(
                    DefaultErrorCodes.VALUE_INVALID_FORMAT, "countryId",
                    "Invalid ISO 3166 country code: " + countryId);
        }
    }

    @Override
    public void validateCountryIds(List<String> countryIds) {
        if (countryIds != null) {
            if (countryIds.size() > MAX_COUNTRY_IDS) {
                throw new BadJsonApiRequestException(
                        DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG, "countryIds",
                        "Country IDs list must not exceed " + MAX_COUNTRY_IDS + " items");
            }
            ListUtils.emptyIfNull(countryIds).forEach(id -> validateNotBlank(id, "countryIds"));
        }
    }

    @Override
    public void validateRegion(String region) {
        validateNotBlank(region, "region");
        if (Region.fromName(region) == null) {
            throw new BadJsonApiRequestException(
                    DefaultErrorCodes.VALUE_INVALID_FORMAT, "region",
                    "Unsupported region: " + region);
        }
    }

    private void validateNotBlank(String value, String paramName) {
        if (StringUtils.isBlank(value)) {
            throw new BadJsonApiRequestException(
                    DefaultErrorCodes.VALUE_IS_ABSENT, paramName, paramName + " must not be blank");
        }
    }

}
