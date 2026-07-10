package pro.api4.jsonapi4j.sampleapp.operations.cache;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A plain JAX-RS resource that returns hardcoded JSON:API responses with configurable Cache-Control headers.
 * Used as a downstream target for compound docs resolution during cache integration tests (mirrors the
 * Spring sample app's {@code CacheTestController}).
 * <p>
 * The compound docs HTTP client calls: {@code GET /test-api/countries?filter[id]=US,NO&...}.
 * This resource parses the {@code filter[id]} param and returns matching hardcoded resources, bumping the
 * per-type counter in {@link QuarkusInvocationTracker}.
 */
@Path("/test-api")
public class QuarkusCacheTestResource {

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

    @Inject
    QuarkusInvocationTracker invocationTracker;

    @GET
    @Path("/countries")
    @Produces(JSON_API_MEDIA_TYPE)
    public Response getCountries(@QueryParam("filter[id]") String filterIds,
                                 @QueryParam("x-cache-control") String cacheControl) {
        invocationTracker.increment("countries");
        String body = buildResponse(filterIds, COUNTRIES);
        return buildResponse(body, cacheControl != null ? cacheControl : "max-age=300");
    }

    @GET
    @Path("/currencies")
    @Produces(JSON_API_MEDIA_TYPE)
    public Response getCurrencies(@QueryParam("filter[id]") String filterIds,
                                  @QueryParam("x-cache-control") String cacheControl) {
        invocationTracker.increment("currencies");
        String body = buildResponse(filterIds, CURRENCIES);
        return buildResponse(body, cacheControl != null ? cacheControl : "max-age=60");
    }

    private String buildResponse(String filterIds, Map<String, String> dataStore) {
        String dataArray = Arrays.stream(filterIds.split(","))
                .map(String::trim)
                .filter(dataStore::containsKey)
                .map(dataStore::get)
                .collect(Collectors.joining(","));
        return "{\"data\":[" + dataArray + "]}";
    }

    private Response buildResponse(String body, String cacheControlValue) {
        return Response.ok(body)
                .type(JSON_API_MEDIA_TYPE)
                .header("Cache-Control", cacheControlValue)
                .build();
    }

}
