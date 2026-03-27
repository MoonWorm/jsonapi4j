package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.client.JsonCompoundDocsApiHttpClient;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser.IntermediateParseResult;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseWriter;
import pro.api4.jsonapi4j.compound.docs.json.ParseResult;

import java.net.URI;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class CompoundDocsResolver {

    private final CompoundDocsResolverConfig config;
    private final DomainUrlResolver domainUrlResolver;

    private final JsonCompoundDocsApiHttpClient httpClient;

    private final JsonApiResponseParser jsonApiResponseParser;
    private final JsonApiResponseWriter jsonApiResponseWriter;

    private final ExecutorService executorService;

    public CompoundDocsResolver(CompoundDocsResolverConfig config,
                                DomainUrlResolver domainUrlResolver,
                                ObjectMapper objectMapper,
                                ExecutorService executorService) {
        Validate.notNull(config, "CompoundDocsResolverConfig is not configured");
        Validate.notNull(domainUrlResolver, "DomainUrlResolver is not configured");
        Validate.notNull(objectMapper, "ObjectMapper is not configured");
        Validate.notNull(executorService, "ExecutorService is not configured");

        this.config = config;
        this.domainUrlResolver = domainUrlResolver;

        this.httpClient = new JsonCompoundDocsApiHttpClient(objectMapper, config.getErrorStrategy());

        this.jsonApiResponseParser = new JsonApiResponseParser(objectMapper);
        this.jsonApiResponseWriter = new JsonApiResponseWriter(objectMapper);

        this.executorService = executorService;
    }

    public String resolveCompoundDocs(String originalJsonApiResponse,
                                      CompoundDocsRequest compoundDocsRequest) {
        String relationshipName = compoundDocsRequest.getRelationshipNameFromRequestUri();
        if (relationshipName == null) {
            return resolveCompoundDocsForPrimaryResourceResponse(
                    originalJsonApiResponse,
                    compoundDocsRequest
            );
        }
        return resolveCompoundDocsForRelationshipResponse(
                originalJsonApiResponse,
                compoundDocsRequest,
                relationshipName
        );
    }

    public String resolveCompoundDocsForPrimaryResourceResponse(
            String originalJsonApiResponse,
            CompoundDocsRequest compoundDocsRequest
    ) throws ErrorJsonApiResponseException {
        if (compoundDocsRequest.isProcessable()) {
            return resolveCompoundDocsInternal(
                    originalJsonApiResponse,
                    compoundDocsRequest.includes(),
                    compoundDocsRequest,
                    () -> jsonApiResponseParser.parsePrimaryResourceDoc(originalJsonApiResponse)
            );
        }
        return originalJsonApiResponse;
    }

    public String resolveCompoundDocsForRelationshipResponse(String originalJsonApiResponse,
                                                             CompoundDocsRequest compoundDocsRequest,
                                                             String relationshipName) throws ErrorJsonApiResponseException {
        if (compoundDocsRequest.isProcessable()) {
            List<String> effectiveOriginalRequestIncludes =
                    compoundDocsRequest.includes() == null ?
                            Collections.emptyList() :
                            compoundDocsRequest.includes()
                                    .stream()
                                    .filter(i -> i.startsWith(relationshipName))
                                    .toList();
            return resolveCompoundDocsInternal(
                    originalJsonApiResponse,
                    effectiveOriginalRequestIncludes,
                    compoundDocsRequest,
                    () -> jsonApiResponseParser.parseRelationshipDoc(originalJsonApiResponse, relationshipName)
            );
        }
        return originalJsonApiResponse;
    }

    private CompletableFuture<List<String>> sendJsonApiRequestAsync(Set<String> ids,
                                                                    String resourceType,
                                                                    Set<String> requestIncludes,
                                                                    CompoundDocsRequest originalRequest) {
        URI domainUri = resolveDomainUrl(resourceType);
        return CompletableFuture.supplyAsync(
                () -> httpClient.doBatchFetch(
                        domainUri,
                        resourceType,
                        ids,
                        requestIncludes,
                        originalRequest,
                        config
                ),
                executorService
        );
    }

    private String resolveCompoundDocsInternal(String originalJsonApiResponse,
                                               List<String> effectiveRequestIncludes,
                                               CompoundDocsRequest request,
                                               Supplier<ParseResult> parseResultSupplier) throws ErrorJsonApiResponseException {

        ParseResult originalParseResult = parseResultSupplier.get();
        Set<String> allResources = new HashSet<>();
        int currentLevel = 1;
        Map<String, Set<String>> nextLevelIncludes = getNextLevelIncludes(effectiveRequestIncludes, currentLevel);
        boolean hasNextHops = !nextLevelIncludes.isEmpty();

        Set<IntermediateParseResult> currentLevelParseResults = Collections.singleton(
                new IntermediateParseResult(
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
                futures.add(sendJsonApiRequestAsync(ids, resourceType, requestIncludes, request));
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
            nextLevelIncludes = getNextLevelIncludes(effectiveRequestIncludes, currentLevel);
            hasNextHops = !nextLevelIncludes.isEmpty();
        }

        if (!allResources.isEmpty()) {
            return jsonApiResponseWriter.composeWithIncludedMember(
                    (ObjectNode) originalParseResult.rootNode(),
                    allResources
            );
        }

        return originalJsonApiResponse;
    }

    private URI resolveDomainUrl(String resourceType) {
        try {
            URI domainUri = domainUrlResolver.getDomainUrl(resourceType);
            if (domainUri == null) {
                throw new NullPointerException("DomainUrlResolver returned a null URI");
            }
            return domainUri;
        } catch (Exception e) {
            throw new DomainResolutionException("Error resolving domain url", e);
        }
    }

    private Set<String> resolveIncludesToRequest(Map<String, Set<String>> typeToRelationshipsName,
                                                 Map<String, Set<String>> nextLevelIncludes,
                                                 String resourceType) {
        return typeToRelationshipsName.get(resourceType)
                .stream()
                .flatMap(rel -> nextLevelIncludes.get(rel).stream())
                .collect(Collectors.toSet());
    }

    private Map<String, Set<String>> getNextLevelIncludes(List<String> includes, int currentLevel) {
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

    private Map<String, Set<String>> getResourceIdsByType(Set<IntermediateParseResult> parseResults) {
        Map<String, Set<String>> resourceIdsByType = new HashMap<>();
        for (IntermediateParseResult parseResult : parseResults) {
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

    private Map<String, Set<String>> getTypeToRelationships(Set<IntermediateParseResult> parseResults) {
        Map<String, Set<String>> typeToRelationshipName = new HashMap<>();
        for (IntermediateParseResult parseResult : parseResults) {
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
