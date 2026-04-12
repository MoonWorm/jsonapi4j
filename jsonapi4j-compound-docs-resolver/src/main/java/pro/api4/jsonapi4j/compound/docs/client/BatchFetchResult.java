package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.compound.docs.cache.CacheControlDirectives;

import java.util.List;

/**
 * Result of a batch resource fetch, potentially combining cache hits with
 * resources fetched via HTTP.
 *
 * @param resources  the merged list of resource JSON strings (cache hits + HTTP results)
 * @param directives the most restrictive Cache-Control directives from this fetch
 *                   (merged from HTTP response and cache hit TTLs), or {@code null}
 *                   if no caching information is available
 */
public record BatchFetchResult(List<String> resources, CacheControlDirectives directives) {
}
