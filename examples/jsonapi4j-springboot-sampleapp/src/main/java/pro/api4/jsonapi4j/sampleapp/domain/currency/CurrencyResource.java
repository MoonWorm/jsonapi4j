package pro.api4.jsonapi4j.sampleapp.domain.currency;

import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasResourceInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCurrencyWithCode;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.CURRENCIES;

@OasResourceInfo(
        attributes = CurrencyAttributes.class
)
@Component
public class CurrencyResource implements Resource<DownstreamCurrencyWithCode> {

    @Override
    public String resolveResourceId(DownstreamCurrencyWithCode downstreamCurrency) {
        return downstreamCurrency.getCode();
    }

    @Override
    public ResourceType resourceType() {
        return CURRENCIES;
    }

    @Override
    public CurrencyAttributes resolveAttributes(DownstreamCurrencyWithCode downstreamCurrency) {
        return new CurrencyAttributes(
                downstreamCurrency.getDownstreamCurrency().getName(),
                downstreamCurrency.getDownstreamCurrency().getSymbol()
        );
    }

}
