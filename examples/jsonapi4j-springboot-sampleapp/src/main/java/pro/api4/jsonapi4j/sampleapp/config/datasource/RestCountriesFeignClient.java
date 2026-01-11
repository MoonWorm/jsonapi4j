package pro.api4.jsonapi4j.sampleapp.config.datasource;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;

import java.util.List;

@FeignClient(name = "restCountriesClient", url = "${integrations.rest-countries.url:https://restcountries.com/v3.1}")
public interface RestCountriesFeignClient extends CountriesClient {

    @Override
    @GetMapping(value = "/region/{region}", produces = MediaType.APPLICATION_JSON_VALUE)
    List<DownstreamCountry> getCountriesByRegion(@PathVariable Region region,
                                                 @RequestParam(required = false) List<Field> fields);

    @Override
    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    List<DownstreamCountry> getAllCountries(@RequestParam(required = false) List<Field> fields);

    @Override
    @GetMapping(value = "/alpha", produces = MediaType.APPLICATION_JSON_VALUE)
    List<DownstreamCountry> getByCca2(@RequestParam List<String> codes,
                                      @RequestParam(required = false) List<Field> fields);

    @Override
    @GetMapping(value = "/currency/{currency}", produces = MediaType.APPLICATION_JSON_VALUE)
    List<DownstreamCountry> getByCurrency(@PathVariable String currency,
                                          @RequestParam(required = false) List<Field> fields);

}
