package pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries;

import lombok.Data;

@Data
public class DownstreamCurrency {

    private final String name;
    private final String symbol;

    public DownstreamCurrency(String name, String symbol) {
        this.name = name;
        this.symbol = symbol;
    }

}
