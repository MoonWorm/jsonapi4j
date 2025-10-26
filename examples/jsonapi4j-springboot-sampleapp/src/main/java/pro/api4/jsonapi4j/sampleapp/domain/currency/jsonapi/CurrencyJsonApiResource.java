package pro.api4.jsonapi4j.sampleapp.domain.currency.jsonapi;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.plugin.oas.ResourceOasPlugin;
import pro.api4.jsonapi4j.plugin.ResourcePlugin;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyAttributes;
import org.springframework.stereotype.Component;

import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.CURRENCIES;

@Component
public class CurrencyJsonApiResource implements Resource<CurrencyAttributes, DownstreamCurrencyWithCode> {

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

    @Override
    public List<ResourcePlugin<?>> plugins() {
        return List.of(
                ResourceOasPlugin.builder()
                        .attributes(CurrencyAttributes.class)
                        .build()
        );
    }
}
