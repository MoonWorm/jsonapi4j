package pro.api4.jsonapi4j.sampleapp.config.datasource.model.country;

import lombok.Data;

@Data
public class DownstreamCurrencyWithCode {
    private final String code;
    private final DownstreamCurrency downstreamCurrency;
}
