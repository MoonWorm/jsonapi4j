package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.compound.docs.IncompleteReason;
import pro.api4.jsonapi4j.compound.docs.json.ParsedResource;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;

import java.util.List;

/**
 * Result of fetching resources of a type - see {@link BatchFetcher}.
 *
 * @param resources        the fetched resources, parsed
 * @param directives       the {@code Cache-Control} directives of the fetch - governing how long its resources may be
 *                         cached and how cacheable the final compound document is; {@code null} is taken as
 *                         {@link CacheControlDirectives#NON_CACHEABLE}, just like an absent header
 * @param incompleteReason why some of the requested resources are missing - their fetch failed or was skipped, and
 *                         that was ignored - or {@code null} when none are
 */
public record FetchResult(List<ParsedResource> resources,
                          CacheControlDirectives directives,
                          IncompleteReason incompleteReason) {

    public FetchResult {
        directives = directives == null ? CacheControlDirectives.NON_CACHEABLE : directives;
    }

    public FetchResult(List<ParsedResource> resources, CacheControlDirectives directives) {
        this(resources, directives, null);
    }

    /**
     * @return the result of fetching nothing
     */
    public static FetchResult empty() {
        return new FetchResult(List.of(), null);
    }

    /**
     * @return the result of a failed fetch that is being ignored. It carries {@code no-store}: a compound document
     * missing these resources must not be stored by any cache.
     */
    public static FetchResult ignoredFailure() {
        return new FetchResult(List.of(), CacheControlDirectives.NO_STORE, IncompleteReason.FETCH_FAILED);
    }

    /**
     * @return the result for resources that are skipped without a fetch, because there is no route to their type.
     * It carries {@code no-store}: a compound document missing these resources must not be stored by any cache.
     */
    public static FetchResult skipped() {
        return new FetchResult(List.of(), CacheControlDirectives.NO_STORE, IncompleteReason.NO_ROUTE);
    }

}
