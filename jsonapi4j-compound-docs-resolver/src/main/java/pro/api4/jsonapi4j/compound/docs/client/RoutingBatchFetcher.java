package pro.api4.jsonapi4j.compound.docs.client;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.cache.CompoundDocsResourceCache;
import pro.api4.jsonapi4j.compound.docs.config.CompoundDocsResolverConfig;
import pro.api4.jsonapi4j.compound.docs.exception.DomainResolutionException;
import pro.api4.jsonapi4j.compound.docs.json.JsonApiResponseParser;

import java.util.concurrent.Executor;

/**
 * Fetches each batch through the chain for the kind of its route - the only place that tells the kinds apart.
 * {@link #compose} assembles the chains the resolver uses:
 * <pre>
 * OverHttp  → Caching → Chunking → ErrorStrategyAware → HTTP fetcher
 * InProcess →           Chunking → ErrorStrategyAware → in-process fetcher
 * </pre>
 * with {@link CachingBatchFetcher} only when a cache is configured, then {@link ChunkingBatchFetcher}, and
 * {@link ErrorStrategyAwareBatchFetcher} around each single request.
 * Resources fetched in-process are never cached: reading them is cheap, and they are always fresh.
 */
public class RoutingBatchFetcher implements BatchFetcher<DomainSettings> {

    /**
     * Stands in for the in-process fetcher of a resolver that has none, e.g. one embedded in an API gateway, where
     * every resource type is served elsewhere. Reaching it means a
     * {@link pro.api4.jsonapi4j.compound.docs.DomainSettingsResolver} routed a type in-process anyway - which the
     * resolver already reports as a missing route while routing.
     */
    private static final BatchFetcher<DomainSettings.InProcess> NO_IN_PROCESS_FETCHER = (batch, originalRequest) -> {
        throw new DomainResolutionException(String.format(
                "Resource type '%s' is routed in-process, but no in-process fetcher is configured",
                batch.resourceType()
        ));
    };

    private final BatchFetcher<DomainSettings.OverHttp> overHttp;
    private final BatchFetcher<DomainSettings.InProcess> inProcess;

    /**
     * @param overHttp  fetches resources of services over HTTP
     * @param inProcess fetches resources the app serves itself
     */
    RoutingBatchFetcher(BatchFetcher<DomainSettings.OverHttp> overHttp,
                        BatchFetcher<DomainSettings.InProcess> inProcess) {
        this.overHttp = Validate.notNull(overHttp, "overHttp must not be null");
        this.inProcess = Validate.notNull(inProcess, "inProcess must not be null");
    }

    /**
     * A fetcher of a resolver without resources of its own: it fetches everything over HTTP.
     */
    RoutingBatchFetcher(BatchFetcher<DomainSettings.OverHttp> overHttp) {
        this(overHttp, NO_IN_PROCESS_FETCHER);
    }

    /**
     * @param httpFetcher      reaches downstream services
     * @param inProcessFetcher reaches the resources the app serves itself, or {@code null} when there are none
     * @param cache            caches resources fetched over HTTP, or {@code null} for no caching
     */
    public static RoutingBatchFetcher compose(BatchFetcher<DomainSettings.OverHttp> httpFetcher,
                                              BatchFetcher<DomainSettings.InProcess> inProcessFetcher,
                                              CompoundDocsResourceCache cache,
                                              JsonApiResponseParser responseParser,
                                              Executor executor,
                                              CompoundDocsResolverConfig config) {
        BatchFetcher<DomainSettings.OverHttp> overHttp = chunked(httpFetcher, executor, config);
        if (cache != null) {
            overHttp = new CachingBatchFetcher<>(overHttp, cache, responseParser, config.getPropagation());
        }
        return inProcessFetcher == null
                ? new RoutingBatchFetcher(overHttp)
                : new RoutingBatchFetcher(overHttp, chunked(inProcessFetcher, executor, config));
    }

    private static <R extends DomainSettings> BatchFetcher<R> chunked(BatchFetcher<R> fetcher,
                                                                      Executor executor,
                                                                      CompoundDocsResolverConfig config) {
        return new ChunkingBatchFetcher<>(
                new ErrorStrategyAwareBatchFetcher<>(fetcher, config.getErrorStrategy()),
                executor
        );
    }

    @Override
    public FetchResult fetch(BatchFetch<DomainSettings> batch, CompoundDocsRequest originalRequest) {
        if (batch.domainSettings() instanceof DomainSettings.InProcess route) {
            return inProcess.fetch(batch.withDomainSettings(route), originalRequest);
        }
        if (batch.domainSettings() instanceof DomainSettings.OverHttp route) {
            return overHttp.fetch(batch.withDomainSettings(route), originalRequest);
        }
        throw new IllegalArgumentException("Unknown kind of route: " + batch.domainSettings());
    }

    /**
     * @return whether resources of a type the app serves itself can be fetched - see
     * {@link DomainSettings#inProcess(int)}
     */
    public boolean isInProcessFetcherConfigured() {
        return inProcess != NO_IN_PROCESS_FETCHER;
    }

}
