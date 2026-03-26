package pro.api4.jsonapi4j.compound.docs.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class JsonApiHttpClient {

    public static final String X_DISABLE_COMPOUND_DOCS = "X-Disable-Compound-Docs";
    private static final Set<String> DISALLOWED_HEADERS = Set.of(
            "connection",
            "content-length",
            "expect",
            "host",
            "upgrade"
    );
    private final ObjectMapper objectMapper;
    private final ErrorStrategy errorStrategy;

    public JsonApiHttpClient(ObjectMapper objectMapper,
                             ErrorStrategy errorStrategy) {
        this.objectMapper = objectMapper;
        this.errorStrategy = errorStrategy;
    }

    public List<String> doBatchFetch(URI domainBaseUrl,
                                     String resourceType,
                                     Set<String> ids,
                                     Set<String> includes,
                                     Map<String, List<String>> fields,
                                     Map<String, String> originalRequestHeaders) {
        try (HttpClient client = HttpClient.newHttpClient()) {

            String uri = domainBaseUrl.toString();
            if (uri.endsWith("/")) {
                uri += resourceType;
            } else {
                uri += "/" + resourceType;
            }
            uri += "?filter[id]=" + String.join(",", ids.stream().sorted().toList());
            if (includes != null && !includes.isEmpty()) {
                uri += "&include=" + String.join(",", includes);
            }
            if (fields != null && !fields.isEmpty()) {
                uri += "&" + fields.entrySet()
                        .stream()
                        .map(e -> String.format("fields[%s]", e.getKey()) + "=" + String.join(",", e.getValue()))
                        .collect(Collectors.joining("&"));
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            if (originalRequestHeaders != null) {
                originalRequestHeaders.forEach((header, value) -> {
                    if (!DISALLOWED_HEADERS.contains(header.toLowerCase())) {
                        requestBuilder.header(header, value);
                    }
                });
            }
            requestBuilder.header(X_DISABLE_COMPOUND_DOCS, String.valueOf(true));
            HttpRequest request = requestBuilder.uri(URI.create(uri)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (errorStrategy == ErrorStrategy.IGNORE) {
                    return Collections.emptyList();
                } else {
                    throw new ErrorJsonApiResponseException("Got error response from a downstream service on GET " + uri + " url");
                }
            }
            return parseResponse(response);
        } catch (IOException | InterruptedException e) {
            throw new ErrorJsonApiResponseException("Error during sending HTTP request to resolve JSON:API Compound Docs", e);
        }
    }

    private List<String> parseResponse(HttpResponse<String> response) {
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
                List<String> result = new ArrayList<>();
                for (JsonNode node : dataNode) {
                    result.add(objectMapper.writeValueAsString(node));
                }
                return result;
            } else if (dataNode.isObject()) {
                return Collections.singletonList(objectMapper.writeValueAsString(dataNode));
            }
            return Collections.emptyList();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
