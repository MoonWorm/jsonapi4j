package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetch;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetchResult;
import pro.api4.jsonapi4j.compound.docs.client.CachingCompoundDocsFetcher;
import pro.api4.jsonapi4j.compound.docs.client.JsonApi4jCompoundDocsApiHttpClient;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseWriter;
import pro.api4.jsonapi4j.compound.docs.json.ParseResult;
import pro.api4.jsonapi4j.compound.docs.json.PrimaryResource;
import pro.api4.jsonapi4j.compound.docs.json.ResourceLinkage;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

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

    private final CachingCompoundDocsFetcher fetcher;

    private final JsonApiResponseParser jsonApiResponseParser;
    private final JsonApiResponseWriter jsonApiResponseWriter;

    private final ExecutorService executorService;

    public CompoundDocsResolver(CompoundDocsResolverConfig config,
                                ObjectMapper objectMapper,
                                ExecutorService executorService) {
        this(config, objectMapper, executorService, (CompoundDocsResourceCache) null);
    }

    /**
     * Creates a resolver with caching support.
     *
     * @param cache the resource cache, or {@code null} to disable caching
     */
    public CompoundDocsResolver(CompoundDocsResolverConfig config,
                                ObjectMapper objectMapper,
                                ExecutorService executorService,
                                CompoundDocsResourceCache cache) {
        this(
                config,
                objectMapper,
                executorService,
                new CachingCompoundDocsFetcher(
                        new JsonApi4jCompoundDocsApiHttpClient(
                                Validate.notNull(objectMapper, "ObjectMapper is not configured"),
                                Validate.notNull(config, "CompoundDocsResolverConfig is not configured")
                        ),
                        cache,
                        Validate.notNull(executorService, "ExecutorService is not configured"),
                        config
                )
        );
    }

    CompoundDocsResolver(CompoundDocsResolverConfig config,
                         ObjectMapper objectMapper,
                         ExecutorService executorService,
                         CachingCompoundDocsFetcher fetcher) {
        Validate.notNull(config, "CompoundDocsResolverConfig is not configured");

        Validate.notNull(objectMapper, "ObjectMapper is not configured");
        Validate.notNull(executorService, "ExecutorService is not configured");
        Validate.notNull(fetcher, "CachingCompoundDocsFetcher is not configured");

        this.config = config;
        this.fetcher = fetcher;

        this.jsonApiResponseParser = new JsonApiResponseParser(objectMapper);
        this.jsonApiResponseWriter = new JsonApiResponseWriter(objectMapper);

        this.executorService = executorService;
    }

    /**
     * Enriches {@code originalJsonApiResponse} with the {@code included} member the request asks for.
     *
     * @param domainSettingsResolver where each included resource type is fetched from. Passed per call, as routing may
     *                               depend on the request - the CD plugin falls back to the app itself at the local port
     *                               the request arrived on - while a gateway simply passes the same resolver each time
     */
    public CompoundDocsResult resolveCompoundDocs(String originalJsonApiResponse,
                                                  CompoundDocsRequest compoundDocsRequest,
                                                  DomainSettingsResolver domainSettingsResolver) {
        Validate.notNull(domainSettingsResolver, "DomainSettingsResolver must not be null");
        if (!compoundDocsRequest.isProcessable()) {
            return new CompoundDocsResult(originalJsonApiResponse, null);
        }
        String relationshipName = compoundDocsRequest.getRelationshipNameFromRequestUri();
        if (relationshipName == null) {
            return resolveCompoundDocsInternal(
                    originalJsonApiResponse,
                    jsonApiResponseParser.parsePrimaryResourceDoc(originalJsonApiResponse),
                    compoundDocsRequest.getIncludes(),
                    compoundDocsRequest,
                    domainSettingsResolver
            );
        }
        return resolveCompoundDocsInternal(
                originalJsonApiResponse,
                jsonApiResponseParser.parseRelationshipDoc(originalJsonApiResponse, relationshipName),
                includesUnder(relationshipName, compoundDocsRequest),
                compoundDocsRequest,
                domainSettingsResolver
        );
    }

    /**
     * On a relationship endpoint include paths start at the relationship itself, so only those apply.
     */
    private List<String> includesUnder(String relationshipName, CompoundDocsRequest compoundDocsRequest) {
        return compoundDocsRequest.getIncludes()
                .stream()
                .filter(i -> i.startsWith(relationshipName))
                .toList();
    }

    private CompletableFuture<BatchFetchResult> fetchAsync(BatchFetch batch, CompoundDocsRequest originalRequest) {
        return CompletableFuture.supplyAsync(() -> fetcher.fetch(batch, originalRequest), executorService);
    }

    private CompoundDocsResult resolveCompoundDocsInternal(String originalJsonApiResponse,
                                                           ParseResult originalParseResult,
                                                           List<String> effectiveRequestIncludes,
                                                           CompoundDocsRequest request,
                                                           DomainSettingsResolver domainSettingsResolver) throws ErrorJsonApiResponseException {

        IncludeTree includeTree = IncludeTree.of(effectiveRequestIncludes);
        CacheControlAggregator aggregator = new CacheControlAggregator();

        Map<IdAndType, Set<String>> requestedIncludes = new HashMap<>();
        Map<IdAndType, ResourceLinkage> linkages = new HashMap<>();
        Map<IdAndType, String> primaryResourceJsons = new HashMap<>();
        for (PrimaryResource primaryResource : originalParseResult.primaryResources()) {
            ResourceLinkage resourceLinkage = primaryResource.linkage();
            IdAndType idAndType = resourceLinkage.idAndType();
            linkages.put(idAndType, resourceLinkage);
            requestedIncludes.put(idAndType, includeTree.children(IncludeTree.ROOT));
            primaryResourceJsons.put(idAndType, primaryResource.json());
        }
        IncludedResources included = new IncludedResources(config.getDeduplication(), primaryResourceJsons);

        IncludeFrontier frontier = IncludeFrontier.start(includeTree, originalParseResult.relationships());

        int currentLevel = 1;
        while (!frontier.isEmpty()
                && currentLevel <= config.getMaxHops()
                && included.size() <= config.getMaxIncludedResources()) {
            log.debug("Compound docs resolution hop {}, included resources so far: {}", currentLevel, included.size());

            Map<String, Set<String>> idsByType = new HashMap<>();
            Map<String, Set<String>> includesByType = new HashMap<>();
            for (IdAndType resource : frontier.resources()) {
                included.reached(resource);
                Set<String> requiredIncludes = frontier.requiredIncludes(resource);
                Set<String> alreadyRequestedIncludes = requestedIncludes.get(resource);
                if (config.getDeduplication() != Deduplication.NONE
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
                        fetchAsync(
                                new BatchFetch(
                                        resolveDomainSettings(domainSettingsResolver, resourceType),
                                        resourceType,
                                        ids,
                                        typeIncludes
                                ),
                                request
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

    private DomainSettings resolveDomainSettings(DomainSettingsResolver domainSettingsResolver, String resourceType) {
        Optional<DomainSettings> settings;
        try {
            settings = domainSettingsResolver.resolveDomainSettings(resourceType);
        } catch (Exception e) {
            log.warn("Failed to resolve domain settings for resource type '{}': {}", resourceType, e.getMessage());
            throw new DomainResolutionException("Error resolving domain settings", e);
        }
        if (settings == null) {
            throw new DomainResolutionException("DomainSettingsResolver returned null instead of an Optional");
        }
        return settings.orElseThrow(() -> new DomainResolutionException(
                String.format("Resource type '%s' has no mapping", resourceType)
        ));
    }

}
