package pro.api4.jsonapi4j.sampleapp.operations;

import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;

import java.util.List;

public interface CountriesClient {

    List<DownstreamCountry> getCountriesByRegion(Region region, List<Field> fields);

    List<DownstreamCountry> getAllCountries(List<Field> fields);

    List<DownstreamCountry> getByCca2(List<String> codes, List<Field> fields);

    List<DownstreamCountry> getByCurrency(String currency, List<Field> fields);

    enum Field {
        cca2, name, region, currencies
    }

}
