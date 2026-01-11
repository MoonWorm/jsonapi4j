package pro.api4.jsonapi4j.sampleapp.operations;

import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry.Name;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCurrency;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;

import java.util.List;
import java.util.Map;

public class CountriesInMemoryClient implements CountriesClient {

    private static final DownstreamCountry NORWAY = new DownstreamCountry("NO", new Name("Norway"), "Europe", Map.of("NOK", new DownstreamCurrency("krone", "kr")));
    private static final DownstreamCountry FINLAND = new DownstreamCountry("FI", new Name("Finland"), "Europe", Map.of("EUR", new DownstreamCurrency("Euro", "€")));
    private static final DownstreamCountry USA = new DownstreamCountry("US", new Name("United States"), "Americas", Map.of("USD", new DownstreamCurrency("United States dollar", "$")));
    private static final DownstreamCountry TOGO = new DownstreamCountry("TG", new Name("Togo"), "Africa", Map.of("XOF", new DownstreamCurrency("West African CFA franc", "Fr")));
    private static final DownstreamCountry MAYOTTE = new DownstreamCountry("YT", new Name("Mayotte"), "Africa", Map.of("EUR", new DownstreamCurrency("Euro", "€")));

    private static final Map<String, DownstreamCountry> BY_CCA2_CODES = Map.of(
            "NO", NORWAY,
            "FI", FINLAND,
            "US", USA,
            "TG", TOGO,
            "YT", MAYOTTE
    );

    private static final Map<Region, List<DownstreamCountry>> BY_REGION = Map.of(
            Region.europe, List.of(NORWAY, FINLAND),
            Region.americas, List.of(USA),
            Region.africa, List.of(TOGO, MAYOTTE)
    );

    private static final Map<String, List<DownstreamCountry>> BY_CURRENCY = Map.of(
            "NOK", List.of(NORWAY),
            "EUR", List.of(FINLAND, MAYOTTE),
            "USD", List.of(USA),
            "XOF", List.of(TOGO)
    );

    @Override
    public List<DownstreamCountry> getCountriesByRegion(Region region, List<Field> fields) {
        return BY_REGION.get(region);
    }

    @Override
    public List<DownstreamCountry> getAllCountries(List<Field> fields) {
        return List.of(TOGO, MAYOTTE, NORWAY, FINLAND, USA);
    }

    @Override
    public List<DownstreamCountry> getByCca2(List<String> codes, List<Field> fields) {
        return codes.stream().filter(BY_CCA2_CODES::containsKey).map(BY_CCA2_CODES::get).toList();
    }

    @Override
    public List<DownstreamCountry> getByCurrency(String currency, List<Field> fields) {
        return BY_CURRENCY.get(currency);
    }

}
