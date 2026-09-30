package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;

import java.util.List;

/**
 * Result of an HTTP batch fetch from a downstream JSON:API service.
 *
 * @param resources  parsed resources from the response {@code data} member
 * @param directives the response's {@code Cache-Control} directives - governing how long its resources may be cached
 *                   and how cacheable the final compound document is; {@code null} is taken as
 *                   {@link CacheControlDirectives#NON_CACHEABLE}, just like an absent header
 * @param failed     the fetch failed and was ignored per {@link pro.api4.jsonapi4j.compound.docs.config.ErrorStrategy#IGNORE},
 *                   so {@code resources} is empty
 */
public record HttpFetchResult(List<ParsedResource> resources, CacheControlDirectives directives, boolean failed) {

    public HttpFetchResult {
        directives = directives == null ? CacheControlDirectives.NON_CACHEABLE : directives;
    }

    public HttpFetchResult(List<ParsedResource> resources, CacheControlDirectives directives) {
        this(resources, directives, false);
    }

    /**
     * @return the result of a failed fetch that is being ignored. It carries {@code no-store}: a compound document
     * missing these resources must not be stored by any cache.
     */
    public static HttpFetchResult ignoredFailure() {
        return new HttpFetchResult(List.of(), CacheControlDirectives.NO_STORE, true);
    }

}
