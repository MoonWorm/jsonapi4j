package pro.api4.jsonapi4j.compound.docs.client;

import java.util.List;

/**
 * Result of an HTTP batch fetch from a downstream JSON:API service.
 *
 * <p>Contains the parsed resources and the raw {@code Cache-Control} header value
 * from the HTTP response, used by {@link CachingCompoundDocsFetcher} to determine
 * cache storage policy via {@link pro.api4.jsonapi4j.http.cache.CacheControlParser}.
 *
 * @param resources          parsed resources from the response {@code data} member
 * @param cacheControlHeader the raw {@code Cache-Control} header value, or {@code null} if absent
 */
public record HttpFetchResult(List<ParsedResource> resources, String cacheControlHeader) {
}
