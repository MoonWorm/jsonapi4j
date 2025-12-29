package pro.api4.jsonapi4j.sampleapp.config.datasource.model.country;

import lombok.Data;

import java.util.Map;

@Data
public class DownstreamCountry {

    private final String cca2;
    private final Name name;
    private final String region;
    private final Map<String, DownstreamCurrency> currencies;

    public DownstreamCountry(String cca2,
                             Name name,
                             String region,
                             Map<String, DownstreamCurrency> currencies) {
        this.cca2 = cca2;
        this.name = name;
        this.region = region;
        this.currencies = currencies;
    }

    @Data
    public static class Name {

        private final String common;

        public Name(String common) {
            this.common = common;
        }

    }

}
