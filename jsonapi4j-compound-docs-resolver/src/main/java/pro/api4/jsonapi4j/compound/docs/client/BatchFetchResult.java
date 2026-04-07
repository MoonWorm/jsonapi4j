package pro.api4.jsonapi4j.compound.docs.client;

import java.util.List;

/**
 * Result of a batch resource fetch, potentially combining cache hits with
 * resources fetched via HTTP.
 *
 * <p>Returned by {@link CachingCompoundDocsFetcher#fetch}. In Epic 5, this record
 * will be extended with {@code CacheControlDirectives} for response aggregation.
 *
 * @param resources the merged list of resource JSON strings (cache hits + HTTP results)
 */
public record BatchFetchResult(List<String> resources) {
}
