package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.databind.node.ObjectNode;
import pro.api4.jsonapi4j.compound.docs.client.JsonApiHttpClient;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponse;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseWriter;
import pro.api4.jsonapi4j.compound.docs.json.ParseResult;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CompoundDocsResolver {

    private final CompoundDocsResolverConfig config;
    private final JsonApiResponseParser jsonApiResponseParser;
    private final JsonApiHttpClient httpClient;
    private final JsonApiResponseWriter jsonApiResponseWriter;

    public CompoundDocsResolver(CompoundDocsResolverConfig config) {
        this.config = config.validate();
        this.jsonApiResponseParser = new JsonApiResponseParser(config.getObjectMapper());
        this.httpClient = new JsonApiHttpClient(config.getObjectMapper(), config.getErrorStrategy());
        this.jsonApiResponseWriter = new JsonApiResponseWriter(config.getObjectMapper());
    }

    public String resolveCompoundDocsForPrimaryResourceResponse(String originalJsonApiResponse,
                                                             Set<String> originalRequestIncludes,
                                                             Map<String, String> originalRequestHeaders) throws ErrorJsonApiResponse {
        return resolveCompoundDocs(
                originalJsonApiResponse,
                originalRequestIncludes,
                originalRequestHeaders,
                () -> jsonApiResponseParser.parsePrimaryResourceDoc(originalJsonApiResponse)
        );
    }

    public String resolveCompoundDocsForRelationshipResponse(String originalJsonApiResponse,
                                                             Set<String> originalRequestIncludes,
                                                             Map<String, String> originalRequestHeaders,
                                                             String relationshipName) throws ErrorJsonApiResponse {

        Set<String> effectiveOriginalRequestIncludes =
                originalRequestIncludes == null ?
                        Collections.emptySet() :
                        originalRequestIncludes
                                .stream()
                                .filter(i -> i.startsWith(relationshipName))
                                .collect(Collectors.toSet());
        return resolveCompoundDocs(
                originalJsonApiResponse,
                effectiveOriginalRequestIncludes,
                originalRequestHeaders,
                () -> jsonApiResponseParser.parseRelationshipDoc(originalJsonApiResponse, relationshipName)
        );
    }

    private String resolveCompoundDocs(String originalJsonApiResponse,
                                       Set<String> originalRequestIncludes,
                                       Map<String, String> originalRequestHeaders,
                                       Supplier<ParseResult> parseResultSupplier) throws ErrorJsonApiResponse {
        if (isCompoundDocsRequested(originalRequestIncludes)) {
            ParseResult originalParseResult = parseResultSupplier.get();
            Set<String> allResources = new HashSet<>();
            int currentLevel = 1;
            Map<String, Set<String>> nextLevelIncludes = getNextLevelIncludes(originalRequestIncludes, currentLevel);
            boolean hasNextHops = !nextLevelIncludes.isEmpty();

            Set<JsonApiResponseParser.IntermediateParseResult> currentLevelParseResults = Collections.singleton(
                    new JsonApiResponseParser.IntermediateParseResult(
                            originalParseResult.typeToIdsMap(),
                            originalParseResult.typeToRelationshipNameMap()
                    )
            );
            while (hasNextHops && currentLevel <= config.getMaxHops()) {
                Map<String, Set<String>> resourceIdsByType = getResourceIdsByType(currentLevelParseResults);
                Map<String, Set<String>> typeToRelationshipsName = getTypeToRelationships(currentLevelParseResults);

                Set<CompletableFuture<List<String>>> futures = new HashSet<>();
                for (Map.Entry<String, Set<String>> e : resourceIdsByType.entrySet()) {
                    String resourceType = e.getKey();
                    Set<String> ids = e.getValue();
                    Set<String> requestIncludes = resolveIncludesToRequest(
                            typeToRelationshipsName,
                            nextLevelIncludes,
                            resourceType
                    );
                    futures.add(
                            CompletableFuture.supplyAsync(
                                    () -> httpClient.doBatchFetch(
                                            config.getDomainUrlResolver().getDomainUrl(resourceType),
                                            resourceType,
                                            ids,
                                            requestIncludes,
                                            originalRequestHeaders
                                    ),
                                    config.getExecutorService()
                            ));
                }
                Set<String> currentLevelResources = futures.stream()
                        .map(CompletableFuture::join)
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet());

                allResources.addAll(currentLevelResources);

                currentLevelParseResults = currentLevelResources.stream()
                        .map(jsonApiResponseParser::parseResourceDocData)
                        .collect(Collectors.toSet());

                currentLevel++;
                nextLevelIncludes = getNextLevelIncludes(originalRequestIncludes, currentLevel);
                hasNextHops = !nextLevelIncludes.isEmpty();
            }

            if (!allResources.isEmpty()) {
                return jsonApiResponseWriter.composeWithIncludedMember((ObjectNode) originalParseResult.rootNode(), allResources);
            }
        }
        return originalJsonApiResponse;
    }

    private boolean isCompoundDocsRequested(Set<String> originalRequestIncludes) {
        return originalRequestIncludes != null && !originalRequestIncludes.isEmpty();
    }

    private Set<String> resolveIncludesToRequest(Map<String, Set<String>> typeToRelationshipsName,
                                                 Map<String, Set<String>> nextLevelIncludes,
                                                 String resourceType) {
        return typeToRelationshipsName.get(resourceType)
                .stream()
                .flatMap(rel -> nextLevelIncludes.get(rel).stream())
                .collect(Collectors.toSet());
    }

    private Map<String, Set<String>> getNextLevelIncludes(Set<String> includes, int currentLevel) {
        Map<String, Set<String>> nextLevelIncludes = new HashMap<>();
        for (String include : includes) {
            String[] parts = include.split("\\.");
            if (parts.length > currentLevel - 1) {
                String parentRelationship = parts[currentLevel - 1];
                nextLevelIncludes.putIfAbsent(parentRelationship, new HashSet<>());
                if (parts.length > currentLevel) {
                    String nextLevelRelationship = parts[currentLevel];
                    nextLevelIncludes.get(parentRelationship).add(nextLevelRelationship);
                }
            }
        }
        return nextLevelIncludes;
    }

    private Map<String, Set<String>> getResourceIdsByType(Set<JsonApiResponseParser.IntermediateParseResult> parseResults) {
        Map<String, Set<String>> resourceIdsByType = new HashMap<>();
        for (JsonApiResponseParser.IntermediateParseResult parseResult : parseResults) {
            parseResult.typeToIdsMap().forEach((key, value) -> {
                if (resourceIdsByType.containsKey(key)) {
                    resourceIdsByType.get(key).addAll(value);
                } else {
                    resourceIdsByType.put(key, new HashSet<>(value));
                }

            });
        }
        return resourceIdsByType;
    }

    private Map<String, Set<String>> getTypeToRelationships(Set<JsonApiResponseParser.IntermediateParseResult> parseResults) {
        Map<String, Set<String>> typeToRelationshipName = new HashMap<>();
        for (JsonApiResponseParser.IntermediateParseResult parseResult : parseResults) {
            parseResult.typeToRelationshipNamesMap().forEach((key, value) -> {
                if (typeToRelationshipName.containsKey(key)) {
                    typeToRelationshipName.get(key).addAll(value);
                } else {
                    typeToRelationshipName.put(key, new HashSet<>(value));
                }

            });
        }
        return typeToRelationshipName;
    }

}
