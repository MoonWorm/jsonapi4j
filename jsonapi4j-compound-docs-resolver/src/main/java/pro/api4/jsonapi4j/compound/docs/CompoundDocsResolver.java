package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
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
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseWriter;
import pro.api4.jsonapi4j.compound.docs.json.ParseResult;
import pro.api4.jsonapi4j.compound.docs.json.ResourceLinkage;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.function.Supplier;

import static pro.api4.jsonapi4j.http.HttpHeaders.X_DISABLE_COMPOUND_DOCS;

/**
 * Resolves the {@code included} member of a JSON:API document hop by hop.
 *
 * <p>Every resource reached during resolution remembers the include paths it was reached through, and only the
 * relationships continuing one of those paths are followed. Resources of the same type are still fetched in one batch
 * per hop, with the union of the includes their paths need - so a relationship requested on one path never pulls
 * unrequested resources into {@code included} for another path that reaches the same type.
 */
@Slf4j
public class CompoundDocsResolver {

    private final CompoundDocsResolverConfig config;
    private final DomainSettingsResolver domainSettingsResolver;

    private final CachingCompoundDocsFetcher fetcher;

    private final JsonApiResponseParser jsonApiResponseParser;
    private final JsonApiResponseWriter jsonApiResponseWriter;

    private final ExecutorService executorService;

    public CompoundDocsResolver(CompoundDocsResolverConfig config,
                                DomainSettingsResolver domainSettingsResolver,
                                ObjectMapper objectMapper,
                                ExecutorService executorService) {
        this(config, domainSettingsResolver, objectMapper, executorService, (CompoundDocsResourceCache) null);
    }

    /**
     * Creates a resolver with caching support.
     *
     * @param cache the resource cache, or {@code null} to disable caching
     */
    public CompoundDocsResolver(CompoundDocsResolverConfig config,
                                DomainSettingsResolver domainSettingsResolver,
                                ObjectMapper objectMapper,
                                ExecutorService executorService,
                                CompoundDocsResourceCache cache) {
        this(
                config,
                domainSettingsResolver,
                objectMapper,
                executorService,
                new CachingCompoundDocsFetcher(
                        new JsonApi4jCompoundDocsApiHttpClient(
                                Validate.notNull(objectMapper, "ObjectMapper is not configured"),
                                Validate.notNull(config, "CompoundDocsResolverConfig is not configured").getErrorStrategy()
                        ),
                        cache,
                        Validate.notNull(executorService, "ExecutorService is not configured")
                )
        );
    }

    CompoundDocsResolver(CompoundDocsResolverConfig config,
                         DomainSettingsResolver domainSettingsResolver,
                         ObjectMapper objectMapper,
                         ExecutorService executorService,
                         CachingCompoundDocsFetcher fetcher) {
        Validate.notNull(config, "CompoundDocsResolverConfig is not configured");
        Validate.notNull(domainSettingsResolver, "DomainSettingsResolver is not configured");

        Validate.notNull(objectMapper, "ObjectMapper is not configured");
        Validate.notNull(executorService, "ExecutorService is not configured");
        Validate.notNull(fetcher, "CachingCompoundDocsFetcher is not configured");

        this.config = config;
        this.domainSettingsResolver = domainSettingsResolver;
        this.fetcher = fetcher;

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
                    compoundDocsRequest.getIncludes(),
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
                    compoundDocsRequest.getIncludes() == null ?
                            Collections.emptyList() :
                            compoundDocsRequest.getIncludes()
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
        DomainSettings domainSettings = resolveDomainSettings(resourceType, originalRequest.getSelfBaseUrl());
        return CompletableFuture.supplyAsync(
                () -> fetcher.fetch(
                        domainSettings,
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
        IncludeTree includeTree = IncludeTree.of(effectiveRequestIncludes);
        CacheControlAggregator aggregator = new CacheControlAggregator();

        Map<IdAndType, Set<String>> requestedIncludes = new HashMap<>();
        Map<IdAndType, ResourceLinkage> linkages = new HashMap<>();
        IncludedResources included = new IncludedResources(config.isDeduplicateResources());

        IncludeFrontier frontier = IncludeFrontier.start(includeTree, originalParseResult.relationships());

        int currentLevel = 1;
        while (!frontier.isEmpty()
                && currentLevel <= config.getMaxHops()
                && included.size() <= config.getMaxIncludedResources()) {
            log.debug("Compound docs resolution hop {}, included resources so far: {}", currentLevel, included.size());

            Map<String, Set<String>> idsByType = new HashMap<>();
            Map<String, Set<String>> includesByType = new HashMap<>();
            for (IdAndType resource : frontier.resources()) {
                Set<String> requiredIncludes = frontier.requiredIncludes(resource);
                Set<String> alreadyRequestedIncludes = requestedIncludes.get(resource);
                if (config.isDeduplicateResources()
                        && alreadyRequestedIncludes != null
                        && alreadyRequestedIncludes.containsAll(requiredIncludes)) {
                    continue;
                }
                idsByType.computeIfAbsent(resource.getType().getType(), t -> new HashSet<>()).add(resource.getId());
                Set<String> typeIncludes = includesByType.computeIfAbsent(resource.getType().getType(), t -> new HashSet<>());
                typeIncludes.addAll(requiredIncludes);
                if (alreadyRequestedIncludes != null) {
                    typeIncludes.addAll(alreadyRequestedIncludes);
                }
            }

            Map<String, CompletableFuture<BatchFetchResult>> futures = new HashMap<>();
            idsByType.forEach((resourceType, ids) -> {
                Set<String> typeIncludes = includesByType.get(resourceType);
                futures.put(
                        resourceType,
                        sendJsonApiRequestAsync(
                                ids,
                                resourceType,
                                typeIncludes,
                                request,
                                Map.of(X_DISABLE_COMPOUND_DOCS.getName(), String.valueOf(true))
                        )
                );
                log.debug("Queued batch fetch for type '{}', ids: {}, includes: {}", resourceType, ids, typeIncludes);
            });

            for (Map.Entry<String, CompletableFuture<BatchFetchResult>> e : futures.entrySet()) {
                String resourceType = e.getKey();
                BatchFetchResult fetchResult = e.getValue().join();
                aggregator.add(fetchResult.directives());
                idsByType.get(resourceType).forEach(id -> requestedIncludes.put(
                        new IdAndType(id, new ResourceType(resourceType)),
                        includesByType.get(resourceType)
                ));
                for (String resourceJson : fetchResult.resources()) {
                    ResourceLinkage linkage = jsonApiResponseParser.parseResource(resourceJson);
                    if (linkage.idAndType() == null) {
                        log.warn("Skipping a resource of type '{}' without a textual 'type' and 'id': {}", resourceType, resourceJson);
                        continue;
                    }
                    linkages.put(linkage.idAndType(), linkage);
                    included.add(linkage.idAndType(), resourceJson);
                }
            }

            frontier = frontier.next(linkages);
            currentLevel++;
        }

        List<String> includedResources = included.toList();
        log.debug("Compound docs resolution completed. Total hops: {}, total included resources: {}", currentLevel - 1, includedResources.size());

        if (!includedResources.isEmpty()) {
            String responseBody = jsonApiResponseWriter.composeWithIncludedMember(
                    (ObjectNode) originalParseResult.rootNode(),
                    includedResources
            );
            return new CompoundDocsResult(responseBody, aggregator.getResult());
        }

        return new CompoundDocsResult(originalJsonApiResponse, aggregator.getResult());
    }

    private DomainSettings resolveDomainSettings(String resourceType, String selfBaseUrl) {
        try {
            DomainSettings settings = domainSettingsResolver.resolveDomainSettings(resourceType, selfBaseUrl);
            if (settings == null) {
                throw new NullPointerException("DomainSettingsResolver returned null DomainSettings");
            }
            return settings;
        } catch (Exception e) {
            log.warn("Failed to resolve domain settings for resource type '{}': {}", resourceType, e.getMessage());
            throw new DomainResolutionException("Error resolving domain settings", e);
        }
    }

}
