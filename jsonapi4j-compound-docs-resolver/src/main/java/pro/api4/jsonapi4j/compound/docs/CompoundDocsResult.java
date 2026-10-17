package pro.api4.jsonapi4j.compound.docs;

import pro.api4.jsonapi4j.http.cache.CacheControlDirectives;

import java.time.Duration;
import java.util.Map;

/**
 * Result of compound document resolution, containing the response body
 * with included resources and the aggregated Cache-Control directives
 * from all downstream fetches.
 *
 * @param responseBody           the JSON:API response body with included resources
 * @param cacheControlDirectives the aggregated Cache-Control directives (most restrictive
 *                               across all downstream fetches), or {@code null} if no
 *                               caching information is available
 * @param fetchDurations         how long fetching each included resource type took, summed over the hops it was
 *                               fetched in, by resource type. A type still being fetched when the includes deadline
 *                               ran out is missing
 */
public record CompoundDocsResult(String responseBody,
                                 CacheControlDirectives cacheControlDirectives,
                                 Map<String, Duration> fetchDurations) {

    public CompoundDocsResult {
        fetchDurations = fetchDurations == null ? Map.of() : Map.copyOf(fetchDurations);
    }

    public CompoundDocsResult(String responseBody, CacheControlDirectives cacheControlDirectives) {
        this(responseBody, cacheControlDirectives, Map.of());
    }

}
