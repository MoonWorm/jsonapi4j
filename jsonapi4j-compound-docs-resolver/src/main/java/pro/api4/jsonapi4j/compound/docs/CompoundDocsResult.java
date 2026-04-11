package pro.api4.jsonapi4j.compound.docs;

import pro.api4.jsonapi4j.compound.docs.cache.CacheControlDirectives;

/**
 * Result of compound document resolution, containing the response body
 * with included resources and the aggregated Cache-Control directives
 * from all downstream fetches.
 *
 * @param responseBody           the JSON:API response body with included resources
 * @param cacheControlDirectives the aggregated Cache-Control directives (most restrictive
 *                               across all downstream fetches), or {@code null} if no
 *                               caching information is available
 */
public record CompoundDocsResult(String responseBody, CacheControlDirectives cacheControlDirectives) {
}
