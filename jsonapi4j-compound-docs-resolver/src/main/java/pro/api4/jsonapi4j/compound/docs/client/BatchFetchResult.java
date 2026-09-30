package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.compound.docs.IncompleteReason;
import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;

import java.util.List;

/**
 * Result of a batch resource fetch, potentially combining cache hits with
 * resources fetched via HTTP.
 *
 * @param resources  the merged list of resource JSON strings (cache hits + HTTP results)
 * @param directives the most restrictive Cache-Control directives from this fetch
 *                   (merged from HTTP response and cache hit TTLs), or {@code null}
 *                   if no caching information is available
 * @param incompleteReason why some of the requested resources are missing - their fetch failed or was skipped, and
 *                         that was ignored - or {@code null} when none are
 */
public record BatchFetchResult(List<String> resources,
                               CacheControlDirectives directives,
                               IncompleteReason incompleteReason) {

    public BatchFetchResult(List<String> resources, CacheControlDirectives directives) {
        this(resources, directives, null);
    }

    /**
     * @return the result for resources that are skipped without a fetch, because there is no route to their type.
     * It carries {@code no-store}: a compound document missing these resources must not be stored by any cache.
     */
    public static BatchFetchResult skipped() {
        return new BatchFetchResult(List.of(), CacheControlDirectives.NO_STORE, IncompleteReason.NO_ROUTE);
    }

}
