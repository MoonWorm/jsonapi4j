package pro.api4.jsonapi4j.sampleapp.operations.cache;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A plain REST controller that returns hardcoded JSON:API responses with configurable Cache-Control headers.
 * Used as a downstream target for compound docs resolution during cache integration tests.
 * <p>
 * The compound docs HTTP client calls: {@code GET /test-api/countries?filter[id]=US,NO&...}
 * This controller parses the filter[id] param and returns matching hardcoded resources.
 */
@RestController
@RequestMapping("/test-api")
@Profile("cacheTest")
public class CacheTestController {

    private static final String JSON_API_MEDIA_TYPE = "application/vnd.api+json";

    private static final Map<String, String> COUNTRIES = Map.of(
            "US", """
                    {"type":"countries","id":"US","attributes":{"name":"United States","cca2":"US"},"relationships":{"currencies":{"data":[{"type":"currencies","id":"USD"}]}}}""",
            "NO", """
                    {"type":"countries","id":"NO","attributes":{"name":"Norway","cca2":"NO"},"relationships":{"currencies":{"data":[{"type":"currencies","id":"NOK"}]}}}""",
            "FI", """
                    {"type":"countries","id":"FI","attributes":{"name":"Finland","cca2":"FI"},"relationships":{"currencies":{"data":[{"type":"currencies","id":"EUR"}]}}}""",
            "UA", """
                    {"type":"countries","id":"UA","attributes":{"name":"Ukraine","cca2":"UA"},"relationships":{"currencies":{"data":[{"type":"currencies","id":"UAH"}]}}}"""
    );

    private static final Map<String, String> CURRENCIES = Map.of(
            "USD", """
                    {"type":"currencies","id":"USD","attributes":{"name":"United States dollar","symbol":"$"}}""",
            "NOK", """
                    {"type":"currencies","id":"NOK","attributes":{"name":"Norwegian krone","symbol":"kr"}}""",
            "EUR", """
                    {"type":"currencies","id":"EUR","attributes":{"name":"Euro","symbol":"\\u20ac"}}""",
            "UAH", """
                    {"type":"currencies","id":"UAH","attributes":{"name":"Ukrainian hryvnia","symbol":"\\u20b4"}}"""
    );

    @Autowired
    private InvocationTracker invocationTracker;

    @GetMapping(value = "/countries", produces = JSON_API_MEDIA_TYPE)
    public ResponseEntity<String> getCountries(
            @RequestParam(value = "filter[id]") String filterIds,
            @RequestParam(value = "x-cache-control", required = false) String cacheControl) {
        invocationTracker.increment("countries");
        String body = buildResponse(filterIds, COUNTRIES);
        return buildResponseEntity(body, cacheControl != null ? cacheControl : "max-age=300");
    }

    @GetMapping(value = "/currencies", produces = JSON_API_MEDIA_TYPE)
    public ResponseEntity<String> getCurrencies(
            @RequestParam(value = "filter[id]") String filterIds,
            @RequestParam(value = "x-cache-control", required = false) String cacheControl) {
        invocationTracker.increment("currencies");
        String body = buildResponse(filterIds, CURRENCIES);
        return buildResponseEntity(body, cacheControl != null ? cacheControl : "max-age=60");
    }

    private String buildResponse(String filterIds, Map<String, String> dataStore) {
        String dataArray = Arrays.stream(filterIds.split(","))
                .map(String::trim)
                .filter(dataStore::containsKey)
                .map(dataStore::get)
                .collect(Collectors.joining(","));
        return "{\"data\":[" + dataArray + "]}";
    }

    private ResponseEntity<String> buildResponseEntity(String body, String cacheControlValue) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(JSON_API_MEDIA_TYPE));
        headers.set("Cache-Control", cacheControlValue);
        return ResponseEntity.ok().headers(headers).body(body);
    }

}
