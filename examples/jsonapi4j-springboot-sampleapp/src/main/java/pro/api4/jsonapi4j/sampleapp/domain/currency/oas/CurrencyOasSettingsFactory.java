package pro.api4.jsonapi4j.sampleapp.domain.currency.oas;

import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.ParameterConfig;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.idPathParam;

public final class CurrencyOasSettingsFactory {

    public static final String CURRENCY_ID_EXAMPLE = "KRW";

    private CurrencyOasSettingsFactory() {

    }

    public static ParameterConfig currencyIdPathParam() {
        return idPathParam()
                .description("Currency id")
                .example(CURRENCY_ID_EXAMPLE)
                .build();
    }

}
