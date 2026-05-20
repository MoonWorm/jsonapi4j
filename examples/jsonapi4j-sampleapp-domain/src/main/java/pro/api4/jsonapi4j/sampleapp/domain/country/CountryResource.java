package pro.api4.jsonapi4j.sampleapp.domain.country;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;

import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource.COUNTRIES;

@JsonApiResource(resourceType = COUNTRIES)
@OasResourceInfo(
        resourceNameSingle = "country",
        attributes = CountryAttributes.class
)
public class CountryResource implements Resource<DownstreamCountry> {

    public static final String COUNTRIES = "countries";

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
