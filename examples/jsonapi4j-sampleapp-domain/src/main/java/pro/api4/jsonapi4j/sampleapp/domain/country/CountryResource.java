package pro.api4.jsonapi4j.sampleapp.domain.country;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource.COUNTRIES;

@JsonApiResource(resourceType = COUNTRIES)
@OasResourceInfo(
        resourceNameSingle = "country",
        attributes = CountryAttributes.class
)
public class CountryResource implements Resource<DownstreamCountry> {

    public static final String COUNTRIES = "countries";

    private static final Set<String> VALID_CCA2_CODES = Arrays.stream(Locale.getISOCountries())
            .map(String::toUpperCase)
            .collect(Collectors.toSet());

    public static void validateCountryId(String countryId) {
        if (countryId != null && !VALID_CCA2_CODES.contains(countryId.toUpperCase())) {
            throw new JsonApiRequestValidationException(DefaultErrorCodes.VALUE_INVALID_FORMAT, "Not valid CCA2 country code");
        }
    }

    @Override
    public String resolveResourceId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

    @Override
    public CountryAttributes resolveAttributes(DownstreamCountry downstreamCountry) {
        return new CountryAttributes(
                downstreamCountry.getName().getCommon(),
                downstreamCountry.getRegion()
        );
    }

}
