package pro.api4.jsonapi4j.sampleapp.domain.country.jsonapi;

import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryAttributes;
import pro.api4.jsonapi4j.sampleapp.domain.currency.jsonapi.CurrencyResource;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;

@OasResourceInfo(
        attributes = CountryAttributes.class,
        includes = {CurrencyResource.class}
)
@Component
public class CountryResource implements Resource<DownstreamCountry> {

    @Override
    public String resolveResourceId(DownstreamCountry downstreamCountry) {
        return downstreamCountry.getCca2();
    }

    @Override
    public ResourceType resourceType() {
        return COUNTRIES;
    }

    @Override
    public CountryAttributes resolveAttributes(DownstreamCountry downstreamCountry) {
        return new CountryAttributes(
                downstreamCountry.getName().getCommon(),
                downstreamCountry.getRegion()
        );
    }

}
