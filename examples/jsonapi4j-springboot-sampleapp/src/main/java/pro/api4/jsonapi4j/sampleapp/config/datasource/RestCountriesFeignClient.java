package pro.api4.jsonapi4j.sampleapp.config.datasource;

import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "restCountriesClient", url = "${integrations.rest-countries.url:https://restcountries.com/v3.1}")
public interface RestCountriesFeignClient {

    @GetMapping(value = "/region/{region}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DownstreamCountry>> getCountriesByRegion(@PathVariable Region region,
                                                                 @RequestParam(required = false) List<Field> fields);

    @GetMapping(value = "/all", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DownstreamCountry>> getAllCountries(@RequestParam(required = false) List<Field> fields);

    @GetMapping(value = "/alpha", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DownstreamCountry>> getByCca2(@RequestParam List<String> codes,
                                                      @RequestParam(required = false) List<Field> fields);

    @GetMapping(value = "/currency/{currency}", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<DownstreamCountry>> getByCurrency(@PathVariable String currency,
                                                          @RequestParam(required = false) List<Field> fields);


    enum Field {
        cca2, name, region, currencies
    }

}
