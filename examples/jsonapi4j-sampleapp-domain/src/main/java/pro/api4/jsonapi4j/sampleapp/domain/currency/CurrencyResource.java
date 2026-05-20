package pro.api4.jsonapi4j.sampleapp.domain.currency;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCurrencyWithCode;

import static pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource.CURRENCIES;

@JsonApiResource(resourceType = CURRENCIES)
@OasResourceInfo(
        resourceNameSingle = "currency",
        attributes = CurrencyAttributes.class
)
public class CurrencyResource implements Resource<DownstreamCurrencyWithCode> {

    public static final String CURRENCIES = "currencies";

    @Override
    public String resolveResourceId(DownstreamCurrencyWithCode downstreamCurrency) {
        return downstreamCurrency.getCode();
    }

    @Override
    public CurrencyAttributes resolveAttributes(DownstreamCurrencyWithCode downstreamCurrency) {
        return new CurrencyAttributes(
                downstreamCurrency.getDownstreamCurrency().getName(),
                downstreamCurrency.getDownstreamCurrency().getSymbol()
        );
    }

}
