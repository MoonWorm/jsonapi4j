package pro.api4.jsonapi4j.sampleapp.domain.currency;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCurrencyWithCode;

@JsonApiResource(resourceType = "currencies")
@OasResourceInfo(
        resourceNameSingle = "currency",
        attributes = CurrencyAttributes.class
)
public class CurrencyResource implements Resource<DownstreamCurrencyWithCode> {

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
