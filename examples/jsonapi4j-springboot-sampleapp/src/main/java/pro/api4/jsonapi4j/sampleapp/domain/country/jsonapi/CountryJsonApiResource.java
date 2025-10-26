package pro.api4.jsonapi4j.sampleapp.domain.country.jsonapi;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.plugin.oas.ResourceOasPlugin;
import pro.api4.jsonapi4j.plugin.ResourcePlugin;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryAttributes;
import pro.api4.jsonapi4j.sampleapp.domain.currency.jsonapi.CurrencyJsonApiResource;
import org.springframework.stereotype.Component;

import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;

@Component
public class CountryJsonApiResource implements Resource<CountryAttributes, DownstreamCountry> {

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

    @Override
    public List<ResourcePlugin<?>> plugins() {
        return List.of(
                ResourceOasPlugin.builder()
                        .attributes(CountryAttributes.class)
                        .includes(List.of(
                                CurrencyJsonApiResource.class
                        )).build()
        );
    }
}
