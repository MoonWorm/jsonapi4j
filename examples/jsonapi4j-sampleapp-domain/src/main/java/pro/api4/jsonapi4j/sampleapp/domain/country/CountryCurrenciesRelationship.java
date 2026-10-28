package pro.api4.jsonapi4j.sampleapp.domain.country;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.ToManyRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource;

import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryCurrenciesRelationship.CURRENCIES;

@JsonApiRelationship(relationshipName = CURRENCIES, parentResource = CountryResource.class)
@OasRelationshipInfo(
        relationshipTypes = {CurrencyResource.class}
)
public class CountryCurrenciesRelationship implements ToManyRefRelationship<ResourceRef> {

    public static final String CURRENCIES = "currencies";

}
