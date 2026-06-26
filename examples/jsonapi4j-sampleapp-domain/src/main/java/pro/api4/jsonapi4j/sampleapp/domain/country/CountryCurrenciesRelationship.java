package pro.api4.jsonapi4j.sampleapp.domain.country;

import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CurrencyRef;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource;

import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryCurrenciesRelationship.CURRENCIES;

@JsonApiRelationship(relationshipName = CURRENCIES, parentResource = CountryResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CurrencyResource.class}
)
public class CountryCurrenciesRelationship implements ToManyRelationship<CurrencyRef> {

    public static final String CURRENCIES = "currencies";

    @Override
    public String resolveResourceIdentifierType(CurrencyRef currencyRef) {
        return CurrencyResource.CURRENCIES;
    }

    @Override
    public String resolveResourceIdentifierId(CurrencyRef currencyRef) {
        return currencyRef.code();
    }

}
