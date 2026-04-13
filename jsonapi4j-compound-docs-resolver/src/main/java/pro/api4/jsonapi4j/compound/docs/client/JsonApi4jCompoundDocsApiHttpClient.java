package pro.api4.jsonapi4j.compound.docs.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.Propagation;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class JsonApi4jCompoundDocsApiHttpClient {

    private static final Set<String> DISALLOWED_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "upgrade"
    );
    private final ObjectMapper objectMapper;
    private final ErrorStrategy errorStrategy;

    public JsonApi4jCompoundDocsApiHttpClient(ObjectMapper objectMapper,
                                              ErrorStrategy errorStrategy) {
        this.objectMapper = objectMapper;
        this.errorStrategy = errorStrategy;
    }

    public HttpFetchResult doBatchFetch(URI domainBaseUrl,
                                        String resourceType,
                                        Set<String> ids,
                                        Set<String> includes,
                                        CompoundDocsRequest originalRequest,
                                        CompoundDocsResolverConfig config,
                                        Map<String, String> metaHeaders) {
        try (HttpClient client = buildHttpClient(config)) {

            JsonApiUrlBuilder urlBuilder = JsonApiUrlBuilder.from(domainBaseUrl)
                    .resourceType(resourceType)
                    .filterParam("id", ids.stream().sorted().toList())
                    .includeParam(includes);

            if (config.getPropagation().contains(Propagation.FIELDS)) {
                urlBuilder.fieldsParams(originalRequest.fieldSets());
            }
            if (config.getPropagation().contains(Propagation.CUSTOM_QUERY_PARAMS)) {
                urlBuilder.queryParams(originalRequest.customQueryParams());
            }

            String uri = urlBuilder.build();

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            requestBuilder.timeout(Duration.ofMillis(config.getHttpTotalTimeoutMs()));
            if (config.getPropagation().contains(Propagation.HEADERS)) {
                Map<String, String> headers = originalRequest.headers();
                if (headers != null) {
                    headers.forEach((header, value) -> {
                        if (!DISALLOWED_HEADERS.contains(header.toLowerCase())) {
                            requestBuilder.header(header, value);
                        }
                    });
                }
            }

            // add meta headers
            if (metaHeaders != null) {
                metaHeaders.forEach(requestBuilder::header);
            }

            HttpRequest request = requestBuilder.uri(URI.create(uri)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (errorStrategy == ErrorStrategy.IGNORE) {
                    return new HttpFetchResult(Collections.emptyList(), null);
                } else {
                    throw new ErrorJsonApiResponseException("Got error response from a downstream service on GET " + uri + " url");
                }
            }
            List<ParsedResource> resources = parseResponse(response);
            String cacheControlHeader = response.headers()
                    .firstValue("Cache-Control").orElse(null);
            return new HttpFetchResult(resources, cacheControlHeader);
        } catch (Exception e) {
            throw new ErrorJsonApiResponseException("Error during sending HTTP request to resolve JSON:API Compound Docs", e);
        }
    }

    private HttpClient buildHttpClient(CompoundDocsResolverConfig config) {
        return HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(config.getHttpConnectTimeoutMs()))
                .build();
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
        String type = node.has("type") && node.get("type").isTextual()
                ? node.get("type").asText() : null;
        String id = node.has("id") && node.get("id").isTextual()
                ? node.get("id").asText() : null;
        return new ParsedResource(type, id, objectMapper.writeValueAsString(node));
    }

}
