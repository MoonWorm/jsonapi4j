package pro.api4.jsonapi4j.sampleapp.domain.country.oas;

import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.ParameterConfig;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.idPathParam;

public final class CountryOasSettingsFactory {

    public static final String REGION_EXAMPLE = "Asia";
    public static final String COUNTRY_ID_EXAMPLE = "KR";

    private CountryOasSettingsFactory() {

    }

    public static ParameterConfig countryIdPathParam() {
        return idPathParam()
                .description("Country id")
                .example(COUNTRY_ID_EXAMPLE)
                .build();
    }

}
