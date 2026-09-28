package pro.api4.jsonapi4j.compound.docs.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import static pro.api4.jsonapi4j.http.HttpHeaders.X_DISABLE_COMPOUND_DOCS;

@Slf4j
public class JsonApi4jCompoundDocsApiHttpClient {

    private static final Set<String> DISALLOWED_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "upgrade"
    );

    /**
     * Headers that claim a client address are never propagated. An include call is the app calling a service directly -
     * on loopback for same-app types - and a container that trusts loopback as a proxy (e.g. Tomcat's
     * {@code RemoteIpValve}) would otherwise take the caller's own, unverified claim as the remote address.
     */
    private static final Set<String> CLIENT_ADDRESS_HEADERS = Set.of(
            "forwarded",
            "x-forwarded-for",
            "x-real-ip"
    );

    private final ObjectMapper objectMapper;
    private final CompoundDocsResolverConfig config;
    private final HttpClient client;

    /**
     * One {@code HttpClient} is shared by every fetch, deliberately not built per fetch: it owns a connection pool and a
     * selector thread, so a per-fetch instance gives up connection reuse across the chunks a single request fans out
     * into, and only becomes reclaimable once the garbage collector notices it. It also cannot be closed here - the
     * framework targets Java 17, where {@code HttpClient} is not {@link AutoCloseable}.
     */
    public JsonApi4jCompoundDocsApiHttpClient(ObjectMapper objectMapper,
                                              CompoundDocsResolverConfig config) {
        this.objectMapper = objectMapper;
        this.config = config;
        this.client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getHttpConnectTimeoutMs()))
                .build();
    }

    /**
     * Fetches one batch. The request always carries {@code X-Disable-Compound-Docs: true}, since the resolver assembles
     * {@code included} itself and the downstream must not do it again.
     */
    public HttpFetchResult doBatchFetch(BatchFetch batch, CompoundDocsRequest originalRequest) {
        try {
            JsonApiUrlBuilder urlBuilder = JsonApiUrlBuilder.from(batch.domainSettings().url())
                    .resourceType(batch.resourceType())
                    .filterParam("id", batch.ids().stream().sorted().toList())
                    .includeParam(batch.includes());

            if (config.getPropagation().contains(Propagation.FIELDS)) {
                urlBuilder.fieldsParams(originalRequest.getFieldSets());
            }
            if (config.getPropagation().contains(Propagation.CUSTOM_QUERY_PARAMS)) {
                urlBuilder.queryParams(originalRequest.getCustomQueryParams());
            }

            String uri = urlBuilder.build();
            log.debug("Compound docs HTTP request: GET {}", uri);

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            requestBuilder.timeout(Duration.ofMillis(config.getHttpTotalTimeoutMs()));
            if (config.getPropagation().contains(Propagation.HEADERS)) {
                Map<String, String> headers = originalRequest.getHeaders();
                if (headers != null) {
                    headers.forEach((header, value) -> {
                        if (isPropagatable(header)) {
                            requestBuilder.header(header, value);
                        }
                    });
                }
            }

            requestBuilder.header(X_DISABLE_COMPOUND_DOCS.getName(), String.valueOf(true));

            HttpRequest request = requestBuilder.uri(URI.create(uri)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (config.getErrorStrategy() == ErrorStrategy.IGNORE) {
                    log.warn("Non-200 response ({}) from GET {}, ignoring per error strategy", response.statusCode(), uri);
                    return new HttpFetchResult(Collections.emptyList(), null);
                } else {
                    throw new ErrorJsonApiResponseException("Got error response from a downstream service on GET " + uri + " url");
                }
            }
            List<ParsedResource> resources = parseResponse(response);
            log.debug("Compound docs HTTP response: status={}, resources={}", response.statusCode(), resources.size());
            String cacheControlHeader = response.headers()
                    .firstValue("Cache-Control").orElse(null);
            return new HttpFetchResult(resources, cacheControlHeader);
        } catch (Exception e) {
            log.error("HTTP request failed for compound docs resolution: {}", e.getMessage());
            throw new ErrorJsonApiResponseException("Error during sending HTTP request to resolve JSON:API Compound Docs", e);
        }
    }

    static boolean isPropagatable(String header) {
        String name = header.toLowerCase(Locale.ROOT);
        return !DISALLOWED_HEADERS.contains(name) && !CLIENT_ADDRESS_HEADERS.contains(name);
    }

    private List<ParsedResource> parseResponse(HttpResponse<String> response) {
        try {
            JsonNode rootNode = objectMapper.readTree(response.body());
            if (rootNode == null || rootNode.isNull() || !rootNode.isObject()) {
                return Collections.emptyList();
            }
            JsonNode dataNode = rootNode.get("data");
            if (dataNode == null || dataNode.isNull()) {
                return Collections.emptyList();
            }
            if (dataNode.isArray()) {
                List<ParsedResource> result = new ArrayList<>();
                for (JsonNode node : dataNode) {
                    result.add(toParsedResource(node));
                }
                return result;
            } else if (dataNode.isObject()) {
                return Collections.singletonList(toParsedResource(dataNode));
            }
            return Collections.emptyList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private ParsedResource toParsedResource(JsonNode node) throws IOException {
        return new ParsedResource(JsonApiResponseParser.readIdAndType(node), objectMapper.writeValueAsString(node));
    }

}
