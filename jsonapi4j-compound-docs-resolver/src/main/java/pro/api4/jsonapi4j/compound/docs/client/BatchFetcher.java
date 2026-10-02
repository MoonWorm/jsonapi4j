package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.compound.docs.CompoundDocsRequest;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;
import pro.api4.jsonapi4j.compound.docs.exception.DownstreamTimeoutException;
import pro.api4.jsonapi4j.compound.docs.exception.ErrorJsonApiResponseException;
import pro.api4.jsonapi4j.compound.docs.exception.RejectedIncludesException;

/**
 * Fetches resources of a single type by id, for one kind of route {@code R}: {@link DomainSettings.OverHttp}, as
 * {@link JsonApi4jCompoundDocsApiHttpClient} does, or {@link DomainSettings.InProcess} - for a type the app serves
 * itself, which the CD plugin provides, as this module stays independent of the framework. Typed by the kind of route,
 * so fetchers for different kinds can't be mixed up.
 *
 * <p>Fetchers are composed: {@link RoutingBatchFetcher} picks the chain for the kind of route, whose decorators -
 * {@link CachingBatchFetcher}, {@link ChunkingBatchFetcher}, {@link ErrorStrategyAwareBatchFetcher} - add caching,
 * chunking and the error strategy around the fetcher that actually reaches the resources.
 *
 * <p>Fetchers report failures the same way, so the configured strategies apply whichever way a type is reached:
 * {@link ErrorJsonApiResponseException} (or {@link DownstreamTimeoutException}) for a failed fetch, and
 * {@link RejectedIncludesException} for includes the type doesn't support.
 *
 * @param <R> the kind of route
 */
@FunctionalInterface
public interface BatchFetcher<R extends DomainSettings> {

    /**
     * @param batch           the resources to fetch
     * @param originalRequest the original compound docs request, for what is propagated from it
     */
    FetchResult fetch(BatchFetch<R> batch, CompoundDocsRequest originalRequest);

}
