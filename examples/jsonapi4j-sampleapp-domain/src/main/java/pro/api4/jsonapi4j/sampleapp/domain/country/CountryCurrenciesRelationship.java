package pro.api4.jsonapi4j.sampleapp.domain.country;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.CURRENCIES;
import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryRelationshipsRegistry.COUNTRY_CURRENCIES;

@OasRelationshipInfo(
        relationshipTypes = {CurrencyResource.class}
)
public class CountryCurrenciesRelationship implements ToManyRelationship<DownstreamCountry, DownstreamCurrencyWithCode> {

    @Override
    public RelationshipName relationshipName() {
        return COUNTRY_CURRENCIES;
    }

    @Override
    public ResourceType resourceType() {
        return COUNTRIES;
    }

    @Override
    public ResourceType resolveResourceIdentifierType(DownstreamCurrencyWithCode downstreamCurrency) {
        return CURRENCIES;
    }

    @Override
    public String resolveResourceIdentifierId(DownstreamCurrencyWithCode downstreamCurrency) {
        return downstreamCurrency.getCode();
    }

}
