package pro.api4.jsonapi4j.compound.docs.client;

/**
 * A JSON:API resource parsed from a downstream HTTP response, carrying the
 * resource's {@code type} and {@code id} alongside the raw JSON string.
 *
 * <p>Used by {@link CachingCompoundDocsFetcher} to construct {@link pro.api4.jsonapi4j.compound.docs.cache.CacheKey}
 * without re-parsing the JSON.
 *
 * @param type the JSON:API resource type (e.g. {@code "countries"}), may be null for malformed resources
 * @param id   the resource ID (e.g. {@code "FI"}), may be null for malformed resources
 * @param json the raw JSON string of the resource object
 */
public record ParsedResource(String type, String id, String json) {
}
