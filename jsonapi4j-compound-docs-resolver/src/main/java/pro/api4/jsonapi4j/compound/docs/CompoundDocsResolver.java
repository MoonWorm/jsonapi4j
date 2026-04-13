package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetchResult;
import pro.api4.jsonapi4j.compound.docs.client.CachingCompoundDocsFetcher;
import pro.api4.jsonapi4j.compound.docs.client.JsonApi4jCompoundDocsApiHttpClient;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser.IntermediateParseResult;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseWriter;
import pro.api4.jsonapi4j.compound.docs.json.ParseResult;

import java.net.URI;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.http.HttpHeaders.X_DISABLE_COMPOUND_DOCS;

public class CompoundDocsResolver {

    private final CompoundDocsResolverConfig config;
    private final DomainUrlResolver domainUrlResolver;

    private final CachingCompoundDocsFetcher fetcher;

    private final JsonApiResponseParser jsonApiResponseParser;
    private final JsonApiResponseWriter jsonApiResponseWriter;

    private final ExecutorService executorService;

    public CompoundDocsResolver(CompoundDocsResolverConfig config,
                                DomainUrlResolver domainUrlResolver,
                                ObjectMapper objectMapper,
                                ExecutorService executorService) {
        this(config, domainUrlResolver, objectMapper, executorService, null);
    }

    /**
     * Creates a resolver with caching support.
     *
     * @param cache the resource cache, or {@code null} to disable caching
     */
    public CompoundDocsResolver(CompoundDocsResolverConfig config,
                                DomainUrlResolver domainUrlResolver,
                                ObjectMapper objectMapper,
                                ExecutorService executorService,
                                CompoundDocsResourceCache cache) {
        Validate.notNull(config, "CompoundDocsResolverConfig is not configured");
        Validate.notNull(domainUrlResolver, "DomainUrlResolver is not configured");
        Validate.notNull(objectMapper, "ObjectMapper is not configured");
        Validate.notNull(executorService, "ExecutorService is not configured");

        this.config = config;
        this.domainUrlResolver = domainUrlResolver;

        JsonApi4jCompoundDocsApiHttpClient httpClient =
                new JsonApi4jCompoundDocsApiHttpClient(objectMapper, config.getErrorStrategy());
        this.fetcher = new CachingCompoundDocsFetcher(httpClient, cache);

        this.jsonApiResponseParser = new JsonApiResponseParser(objectMapper);
        this.jsonApiResponseWriter = new JsonApiResponseWriter(objectMapper);

        this.executorService = executorService;
    }

    public CompoundDocsResult resolveCompoundDocs(String originalJsonApiResponse,
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

    public CompoundDocsResult resolveCompoundDocsForPrimaryResourceResponse(
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
        return new CompoundDocsResult(originalJsonApiResponse, null);
    }

    public CompoundDocsResult resolveCompoundDocsForRelationshipResponse(String originalJsonApiResponse,
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
        return new CompoundDocsResult(originalJsonApiResponse, null);
    }

    private CompletableFuture<BatchFetchResult> sendJsonApiRequestAsync(Set<String> ids,
                                                                       String resourceType,
                                                                       Set<String> requestIncludes,
                                                                       CompoundDocsRequest originalRequest,
                                                                       Map<String, String> metaHeaders) {
        URI domainUri = resolveDomainUrl(resourceType);
        return CompletableFuture.supplyAsync(
                () -> fetcher.fetch(
                        domainUri,
                        resourceType,
                        ids,
                        requestIncludes,
                        originalRequest,
                        config,
                        metaHeaders
                ),
                executorService
        );
    }

    private CompoundDocsResult resolveCompoundDocsInternal(String originalJsonApiResponse,
                                                          List<String> effectiveRequestIncludes,
                                                          CompoundDocsRequest request,
                                                          Supplier<ParseResult> parseResultSupplier) throws ErrorJsonApiResponseException {

        ParseResult originalParseResult = parseResultSupplier.get();
        Set<String> allResources = new HashSet<>();
        CacheControlAggregator aggregator = new CacheControlAggregator();
        int currentLevel = 1;
        Map<String, Set<String>> nextLevelIncludes = getNextLevelIncludes(effectiveRequestIncludes, currentLevel);
        boolean hasNextHops = !nextLevelIncludes.isEmpty();

        RequestedResourceIdsTracker requestedResourceIdsTracker = new RequestedResourceIdsTracker(
                config.isDeduplicateResources()
        );

        Set<IntermediateParseResult> currentLevelParseResults = Collections.singleton(
                new IntermediateParseResult(
                        originalParseResult.typeToIdsMap(),
                        originalParseResult.typeToRelationshipNameMap()
                )
        );
        while (hasNextHops
                && currentLevel <= config.getMaxHops()
                && allResources.size() <= config.getMaxIncludedResources()) {
            Map<String, Set<String>> resourceIdsByType = getResourceIdsByType(currentLevelParseResults);
            Map<String, Set<String>> typeToRelationshipsName = getTypeToRelationships(currentLevelParseResults);

            Set<CompletableFuture<BatchFetchResult>> futures = new HashSet<>();
            for (Map.Entry<String, Set<String>> e : resourceIdsByType.entrySet()) {

                String resourceType = e.getKey();
                Set<String> ids = e.getValue();

                ids = requestedResourceIdsTracker.calculateNonRequested(resourceType, ids);

                if (!ids.isEmpty()) {
                    Set<String> requestIncludes = resolveIncludesToRequest(
                            typeToRelationshipsName,
                            nextLevelIncludes,
                            resourceType
                    );
                    futures.add(
                            sendJsonApiRequestAsync(
                                    ids,
                                    resourceType,
                                    requestIncludes,
                                    request,
                                    Map.of(X_DISABLE_COMPOUND_DOCS.getName(), String.valueOf(true))
                            )
                    );
                }

            }

            Set<String> currentLevelResources = new HashSet<>();
            for (CompletableFuture<BatchFetchResult> future : futures) {
                BatchFetchResult fetchResult = future.join();
                currentLevelResources.addAll(fetchResult.resources());
                aggregator.add(fetchResult.directives());
            }

            allResources.addAll(currentLevelResources);

            currentLevelParseResults = currentLevelResources.stream()
                    .map(jsonApiResponseParser::parseResourceDocData)
                    .collect(Collectors.toSet());

            currentLevel++;
            nextLevelIncludes = getNextLevelIncludes(effectiveRequestIncludes, currentLevel);
            hasNextHops = !nextLevelIncludes.isEmpty();
        }

        if (!allResources.isEmpty()) {
            String responseBody = jsonApiResponseWriter.composeWithIncludedMember(
                    (ObjectNode) originalParseResult.rootNode(),
                    allResources
            );
            return new CompoundDocsResult(responseBody, aggregator.getResult());
        }

        return new CompoundDocsResult(originalJsonApiResponse, aggregator.getResult());
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
