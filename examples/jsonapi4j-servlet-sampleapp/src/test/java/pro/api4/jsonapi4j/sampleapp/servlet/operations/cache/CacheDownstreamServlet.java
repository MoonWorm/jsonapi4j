package pro.api4.jsonapi4j.sampleapp.servlet.operations.cache;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A plain servlet that returns hardcoded JSON:API responses with configurable Cache-Control headers.
 * Used as a downstream target for compound docs resolution during cache integration tests (mirrors the
 * Spring sample app's {@code CacheTestController}).
 * <p>
 * Mapped at {@code /test-api/*}; the compound docs HTTP client calls
 * {@code GET /test-api/countries?filter[id]=US,NO&...}. This servlet parses the {@code filter[id]} param
 * and returns matching hardcoded resources, bumping the per-type counter in {@link ServletInvocationTracker}.
 */
public class CacheDownstreamServlet extends HttpServlet {

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

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String pathInfo = req.getPathInfo();
        String filterIds = req.getParameter("filter[id]");
        String cacheControl = req.getParameter("x-cache-control");

        String body;
        String cacheControlValue;
        if ("/countries".equals(pathInfo)) {
            ServletInvocationTracker.INSTANCE.increment("countries");
            body = buildResponse(filterIds, COUNTRIES);
            cacheControlValue = cacheControl != null ? cacheControl : "max-age=300";
        } else if ("/currencies".equals(pathInfo)) {
            ServletInvocationTracker.INSTANCE.increment("currencies");
            body = buildResponse(filterIds, CURRENCIES);
            cacheControlValue = cacheControl != null ? cacheControl : "max-age=60";
        } else {
            resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType(JSON_API_MEDIA_TYPE);
        resp.setHeader("Cache-Control", cacheControlValue);
        resp.getWriter().write(body);
    }

    private String buildResponse(String filterIds, Map<String, String> dataStore) {
        String dataArray = Arrays.stream(filterIds.split(","))
                .map(String::trim)
                .filter(dataStore::containsKey)
                .map(dataStore::get)
                .collect(Collectors.joining(","));
        return "{\"data\":[" + dataArray + "]}";
    }

}
