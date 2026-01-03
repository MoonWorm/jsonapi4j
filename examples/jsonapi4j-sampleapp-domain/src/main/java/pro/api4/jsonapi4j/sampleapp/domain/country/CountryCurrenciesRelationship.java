package pro.api4.jsonapi4j.sampleapp.domain.country;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource;

@JsonApiRelationship(relationshipName = "currencies", parentResource = CountryResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CurrencyResource.class}
)
public class CountryCurrenciesRelationship implements ToManyRelationship<DownstreamCountry, DownstreamCurrencyWithCode> {

    @Override
    public String resolveResourceIdentifierType(DownstreamCurrencyWithCode downstreamCurrency) {
        return "currencies";
    }

    @Override
    public String resolveResourceIdentifierId(DownstreamCurrencyWithCode downstreamCurrency) {
        return downstreamCurrency.getCode();
    }

}
