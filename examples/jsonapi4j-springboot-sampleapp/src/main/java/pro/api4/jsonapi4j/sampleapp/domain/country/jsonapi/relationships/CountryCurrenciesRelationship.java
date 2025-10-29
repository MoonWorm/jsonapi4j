package pro.api4.jsonapi4j.sampleapp.domain.country.jsonapi.relationships;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.RelationshipOasPlugin;
import pro.api4.jsonapi4j.plugin.RelationshipPlugin;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCurrencyWithCode;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.CURRENCIES;
import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryRelationshipsRegistry.COUNTRY_CURRENCIES;

@Component
public class CountryCurrenciesRelationship implements ToManyRelationship<DownstreamCountry, DownstreamCurrencyWithCode> {

    @Override
    public RelationshipName relationshipName() {
        return COUNTRY_CURRENCIES;
    }

    @Override
    public ResourceType parentResourceType() {
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

    @Override
    public List<RelationshipPlugin<?>> plugins() {
        return List.of(
                RelationshipOasPlugin.builder()
                        .relationshipTypes(Set.of(CURRENCIES))
                        .build()
        );
    }

}
