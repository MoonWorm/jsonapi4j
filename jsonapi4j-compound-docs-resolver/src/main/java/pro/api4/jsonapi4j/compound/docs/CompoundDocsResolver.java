package pro.api4.jsonapi4j.compound.docs;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.http.cache.CacheControlAggregator;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetch;
import pro.api4.jsonapi4j.compound.docs.client.BatchFetcher;
import pro.api4.jsonapi4j.compound.docs.client.FetchResult;
import pro.api4.jsonapi4j.compound.docs.client.JsonApi4jCompoundDocsApiHttpClient;
import pro.api4.jsonapi4j.compound.docs.client.RoutingBatchFetcher;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.config.Deduplication;
import pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy;
import pro.api4.jsonapi4j.compound.docs.config.UnsupportedIncludeStrategy;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.RejectedIncludesException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseWriter;
import pro.api4.jsonapi4j.compound.docs.json.ParseResult;
import pro.api4.jsonapi4j.compound.docs.json.ParsedResource;
import pro.api4.jsonapi4j.compound.docs.json.ResourceLinkage;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.UnsupportedIncludeException;
import pro.api4.jsonapi4j.exception.UnsupportedIncludeException.UnsupportedInclude;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import java.util.function.Function;
import java.util.stream.Collectors;

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
    private final IncludesChecker includesChecker;

    private final RoutingBatchFetcher fetcher;

    private final JsonApiResponseParser jsonApiResponseParser;
    private final JsonApiResponseWriter jsonApiResponseWriter;

    private final Executor executor;

    /**
     * Creates a resolver that also fetches resource types served in-process - those routed with
     * {@link DomainSettings#inProcess(int)}.
     *
     * @param cache            the resource cache for resources fetched over HTTP, or {@code null} to disable caching
     * @param inProcessFetcher fetches the resources the app serves itself, or {@code null} when there are none
     */
    public CompoundDocsResolver(CompoundDocsResolverConfig config,
                                ObjectMapper objectMapper,
                                Executor executor,
                                CompoundDocsResourceCache cache,
                                BatchFetcher<DomainSettings.InProcess> inProcessFetcher) {
        this(
                config,
                objectMapper,
                executor,
                RoutingBatchFetcher.compose(
                        new JsonApi4jCompoundDocsApiHttpClient(
                                Validate.notNull(objectMapper, "ObjectMapper is not configured"),
                                Validate.notNull(config, "CompoundDocsResolverConfig is not configured")
                        ),
                        inProcessFetcher,
                        cache,
                        new JsonApiResponseParser(objectMapper),
                        Validate.notNull(executor, "Executor is not configured"),
                        config
                )
        );
    }

    CompoundDocsResolver(CompoundDocsResolverConfig config,
                         ObjectMapper objectMapper,
                         Executor executor,
                         RoutingBatchFetcher fetcher) {
        Validate.notNull(config, "CompoundDocsResolverConfig is not configured");

        Validate.notNull(objectMapper, "ObjectMapper is not configured");
        Validate.notNull(executor, "Executor is not configured");
        Validate.notNull(fetcher, "RoutingBatchFetcher is not configured");

        this.config = config;
        this.includesChecker = IncludesChecker.from(config);
        this.fetcher = fetcher;

        this.jsonApiResponseParser = new JsonApiResponseParser(objectMapper);
        this.jsonApiResponseWriter = new JsonApiResponseWriter(objectMapper);

        this.executor = executor;
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
        CheckedIncludes includes = includesChecker.check(compoundDocsRequest);
        String relationshipName = compoundDocsRequest.getRelationshipNameFromRequestUri();
        ParseResult parseResult = relationshipName == null
                ? jsonApiResponseParser.parsePrimaryResourceDoc(originalJsonApiResponse)
                : jsonApiResponseParser.parseRelationshipDoc(originalJsonApiResponse, relationshipName);
        return resolveCompoundDocsInternal(
                originalJsonApiResponse,
                parseResult,
                includes,
                compoundDocsRequest,
                domainSettingsResolver
        );
    }

    private CompoundDocsResult resolveCompoundDocsInternal(String originalJsonApiResponse,
                                                           ParseResult originalParseResult,
                                                           CheckedIncludes includes,
                                                           CompoundDocsRequest request,
                                                           DomainSettingsResolver domainSettingsResolver) throws ErrorJsonApiResponseException {

        IncludeTree includeTree = IncludeTree.of(includes.paths());
        CacheControlAggregator aggregator = new CacheControlAggregator();

        Map<IdAndType, Set<String>> requestedIncludes = new HashMap<>();
        Map<IdAndType, ResourceLinkage> linkages = new HashMap<>();
        Map<IdAndType, String> primaryResourceJsons = new HashMap<>();
        for (ParsedResource primaryResource : originalParseResult.primaryResources()) {
            ResourceLinkage resourceLinkage = primaryResource.linkage();
            IdAndType idAndType = resourceLinkage.idAndType();
            linkages.put(idAndType, resourceLinkage);
            requestedIncludes.put(idAndType, includeTree.children(IncludeTree.ROOT));
            primaryResourceJsons.put(idAndType, primaryResource.json());
        }
        IncludedResources included = new IncludedResources(config.getDeduplication(), primaryResourceJsons);
        Set<IncludedGap> gaps = new HashSet<>(includes.gaps());

        IncludeFrontier frontier = IncludeFrontier.start(includeTree, originalParseResult.relationships());

        int currentLevel = 1;
        while (!frontier.isEmpty()) {
            log.debug("Compound docs resolution hop {}, included resources so far: {}", currentLevel, included.size());

            frontier.resources().forEach(included::reached);
            List<IdAndType> pending = pendingResources(frontier, requestedIncludes);
            if (!pending.isEmpty() && included.size() >= config.getMaxIncludedResources()) {
                log.debug("Stopping compound docs resolution at hop {}: {} included resources reach the maximum of {}",
                        currentLevel, included.size(), config.getMaxIncludedResources());
                gaps.addAll(truncatedPaths(frontier, pending));
                break;
            }

            Map<String, Set<String>> idsByType = new HashMap<>();
            Map<String, Set<String>> includesByType = new HashMap<>();
            for (IdAndType resource : pending) {
                Set<String> requiredIncludes = frontier.requiredIncludes(resource);
                Set<String> alreadyRequestedIncludes = requestedIncludes.get(resource);
                idsByType.computeIfAbsent(resource.getType().getType(), t -> new HashSet<>()).add(resource.getId());
                Set<String> typeIncludes = includesByType.computeIfAbsent(resource.getType().getType(), t -> new HashSet<>());
                typeIncludes.addAll(requiredIncludes);
                if (alreadyRequestedIncludes != null) {
                    typeIncludes.addAll(alreadyRequestedIncludes);
                }
            }

            Map<String, CompletableFuture<FetchResult>> futures = idsByType.keySet().stream()
                    .collect(Collectors.toMap(
                            Function.identity(),
                            resourceType -> fetchAsync(
                                    resourceType,
                                    idsByType.get(resourceType),
                                    includesByType.get(resourceType),
                                    domainSettingsResolver,
                                    request
                            )
                    ));

            for (Map.Entry<String, CompletableFuture<FetchResult>> e : futures.entrySet()) {
                String resourceType = e.getKey();
                TypeFetch typeFetch = awaitFetch(
                        resourceType,
                        idsByType.get(resourceType),
                        includesByType.get(resourceType),
                        e.getValue(),
                        frontier,
                        domainSettingsResolver,
                        request
                );
                FetchResult fetchResult = typeFetch.result();
                aggregator.add(fetchResult.directives());
                if (fetchResult.incompleteReason() != null) {
                    gaps.add(IncludedGap.forType(fetchResult.incompleteReason(), resourceType));
                }
                typeFetch.rejected().forEach(rejected -> gaps.add(
                        IncludedGap.forPath(IncompleteReason.UNSUPPORTED_INCLUDE, rejected.path())
                ));
                idsByType.get(resourceType).forEach(id -> requestedIncludes.put(
                        new IdAndType(id, new ResourceType(resourceType)),
                        typeFetch.includes()
                ));
                for (ParsedResource resource : fetchResult.resources()) {
                    if (resource.idAndType() == null) {
                        log.warn("Skipping a resource of type '{}' without a textual 'type' and 'id': {}", resourceType, resource.json());
                        continue;
                    }
                    linkages.put(resource.idAndType(), resource.linkage());
                    included.add(resource.idAndType(), resource.json());
                }
            }

            frontier = frontier.next(linkages);
            currentLevel++;
        }

        List<String> includedResources = included.toList();
        log.debug("Compound docs resolution completed. Total hops: {}, total included resources: {}", currentLevel - 1, includedResources.size());

        if (!includedResources.isEmpty() || !gaps.isEmpty()) {
            String responseBody = jsonApiResponseWriter.compose(
                    (ObjectNode) originalParseResult.rootNode(),
                    includedResources,
                    gaps
            );
            return new CompoundDocsResult(responseBody, aggregator.getResult());
        }

        return new CompoundDocsResult(originalJsonApiResponse, aggregator.getResult());
    }

    /**
     * Waits for the fetch of {@code ids} of {@code resourceType}. When the downstream service rejects some of
     * {@code includes} as unknown relationships, they are mapped back to the include paths of the request: under
     * {@link UnsupportedIncludeStrategy#FAIL} the request fails naming them, under
     * {@link UnsupportedIncludeStrategy#IGNORE} the resources are fetched again without them.
     *
     * @return the fetch result, the includes it was fetched with, and the include paths rejected on the way
     */
    private TypeFetch awaitFetch(String resourceType,
                                 Set<String> ids,
                                 Set<String> includes,
                                 CompletableFuture<FetchResult> fetch,
                                 IncludeFrontier frontier,
                                 DomainSettingsResolver domainSettingsResolver,
                                 CompoundDocsRequest request) {
        try {
            return new TypeFetch(join(fetch), includes, List.of());
        } catch (RejectedIncludesException e) {
            List<UnsupportedInclude> rejected = e.getRelationshipNames()
                    .stream()
                    .flatMap(relationshipName -> ids.stream()
                            .flatMap(id -> frontier.pathsThrough(new IdAndType(id, new ResourceType(resourceType)), relationshipName).stream())
                            .distinct()
                            .map(path -> new UnsupportedInclude(path, String.format(
                                    "Resource type '%s' has no relationship '%s'", resourceType, relationshipName
                            ))))
                    .toList();
            if (config.getUnsupportedIncludes() == UnsupportedIncludeStrategy.FAIL) {
                throw new UnsupportedIncludeException(rejected);
            }
            log.warn("Resolving '{}' resources without unsupported includes {} per strategy", resourceType, e.getRelationshipNames());
            Set<String> accepted = includes.stream()
                    .filter(include -> !e.getRelationshipNames().contains(include))
                    .collect(Collectors.toSet());
            TypeFetch retried = awaitFetch(
                    resourceType,
                    ids,
                    accepted,
                    fetchAsync(resourceType, ids, accepted, domainSettingsResolver, request),
                    frontier,
                    domainSettingsResolver,
                    request
            );
            return new TypeFetch(
                    retried.result(),
                    retried.includes(),
                    ListUtils.union(rejected, retried.rejected())
            );
        }
    }

    /**
     * @param includes the includes the resources were fetched with
     * @param rejected the include paths the downstream service rejected on the way
     */
    private record TypeFetch(FetchResult result, Set<String> includes, List<UnsupportedInclude> rejected) {
    }

    /**
     * @return the resources of {@code frontier} still to fetch - all of them, unless deduplicating, which skips those
     * already requested with every relationship they need now
     */
    private List<IdAndType> pendingResources(IncludeFrontier frontier, Map<IdAndType, Set<String>> requestedIncludes) {
        return frontier.resources()
                .stream()
                .filter(resource -> {
                    Set<String> alreadyRequestedIncludes = requestedIncludes.get(resource);
                    return config.getDeduplication() == Deduplication.NONE
                            || alreadyRequestedIncludes == null
                            || !alreadyRequestedIncludes.containsAll(frontier.requiredIncludes(resource));
                })
                .toList();
    }

    /**
     * @return a gap for each include path the {@code pending} resources were reached through - resolution stopped
     * before them, so these paths and everything below them are missing
     */
    private Set<IncludedGap> truncatedPaths(IncludeFrontier frontier, List<IdAndType> pending) {
        return pending.stream()
                .flatMap(resource -> frontier.paths(resource).stream())
                .map(path -> IncludedGap.forPath(IncompleteReason.MAX_INCLUDED_RESOURCES, path))
                .collect(Collectors.toSet());
    }

    /**
     * @return the fetch of {@code ids} of {@code resourceType}, or an already {@link FetchResult#skipped() skipped}
     * one when there is no route to the type and {@link ErrorStrategy#IGNORE} lets its resources be skipped
     */
    private CompletableFuture<FetchResult> fetchAsync(String resourceType,
                                                           Set<String> ids,
                                                           Set<String> includes,
                                                           DomainSettingsResolver domainSettingsResolver,
                                                           CompoundDocsRequest originalRequest) {
        Optional<DomainSettings> domainSettings = routeFor(domainSettingsResolver, resourceType);
        if (domainSettings.isEmpty()) {
            return CompletableFuture.completedFuture(FetchResult.skipped());
        }
        BatchFetch<DomainSettings> batch = new BatchFetch<>(domainSettings.get(), resourceType, ids, includes);
        log.debug("Queued batch fetch for type '{}', ids: {}, includes: {}", resourceType, ids, includes);
        return CompletableFuture.supplyAsync(() -> fetcher.fetch(batch, originalRequest), executor);
    }

    /**
     * @return where to fetch {@code resourceType} from, or empty when there is no route and
     * {@link ErrorStrategy#IGNORE} lets its resources be skipped
     */
    private Optional<DomainSettings> routeFor(DomainSettingsResolver domainSettingsResolver, String resourceType) {
        try {
            return Optional.of(fetchable(domainSettingsResolver.requireDomainSettings(resourceType), resourceType));
        } catch (DomainResolutionException e) {
            if (config.getErrorStrategy() == ErrorStrategy.IGNORE) {
                log.error(
                        "Skipping included resources of type '{}' per error strategy - no route to fetch them from: {}",
                        resourceType,
                        e.getMessage()
                );
                return Optional.empty();
            }
            throw e;
        }
    }

    /**
     * A route in-process is usable only when this resolver can fetch in-process - it is created with an in-process
     * {@link BatchFetcher}.
     */
    private DomainSettings fetchable(DomainSettings domainSettings, String resourceType) {
        if (domainSettings instanceof DomainSettings.InProcess && !fetcher.isInProcessFetcherConfigured()) {
            throw new DomainResolutionException(String.format(
                    "Resource type '%s' is routed in-process, but no in-process fetcher is configured", resourceType
            ));
        }
        return domainSettings;
    }

    /**
     * Waits for a fetch, rethrowing its failure as thrown rather than wrapped in a {@link CompletionException}.
     */
    private FetchResult join(CompletableFuture<FetchResult> future) {
        try {
            return future.join();
        } catch (CompletionException e) {
            if (e.getCause() instanceof RuntimeException cause) {
                throw cause;
            }
            throw e;
        }
    }

}
