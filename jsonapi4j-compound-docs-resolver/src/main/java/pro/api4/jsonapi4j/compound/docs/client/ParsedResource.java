package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.processor.IdAndType;

/**
 * A JSON:API resource parsed from a downstream HTTP response, carrying the
 * resource's {@code type} and {@code id} alongside the raw JSON string.
 *
 * <p>Used by {@link CachingCompoundDocsFetcher} to construct {@link pro.api4.jsonapi4j.compound.docs.cache.CacheKey}
 * without re-parsing the JSON.
 *
 * @param idAndType the resource's {@code type} and {@code id}, may be null for malformed resources
 * @param json      the raw JSON string of the resource object
 */
public record ParsedResource(IdAndType idAndType, String json) {
}
