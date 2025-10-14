package pro.api4.jsonapi4j.compound.docs.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsResolverConfig.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponse;

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

public class JsonApiHttpClient {

    public static final String X_DISABLE_COMPOUND_DOCS = "X-Disable-Compound-Docs";

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
                                     Map<String, String> originalRequestHeaders) {
        try (HttpClient client = HttpClient.newHttpClient()) {

            String uri = domainBaseUrl.toString();
            if (uri.endsWith("/")) {
                uri += resourceType;
            } else {
                uri += "/" + resourceType;
            }
            uri += "?filter[id]=" + String.join(",", ids);
            if (includes != null && !includes.isEmpty()) {
                uri += "&include=" + String.join(",", includes);
            }

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder();
            if (originalRequestHeaders != null) {
                originalRequestHeaders.forEach(requestBuilder::header);
            }
            requestBuilder.header(X_DISABLE_COMPOUND_DOCS, String.valueOf(true));
            HttpRequest request = requestBuilder.uri(URI.create(uri)).GET().build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                if (errorStrategy == ErrorStrategy.IGNORE) {
                    return Collections.emptyList();
                } else {
                    throw new ErrorJsonApiResponse("Got error response from a downstream service on GET " + uri + " url");
                }
            }
            return parseResponse(response);
        } catch (IOException | InterruptedException e) {
            throw new ErrorJsonApiResponse("Error during sending HTTP request to resolve JSON:API Compound Docs", e);
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
