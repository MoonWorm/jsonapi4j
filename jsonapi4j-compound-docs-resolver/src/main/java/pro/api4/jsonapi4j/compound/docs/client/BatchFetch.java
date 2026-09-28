package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.compound.docs.DomainSettings;

import java.util.Set;

/**
 * One downstream batch fetch: resources of a single type, by id, with the relationships to request linkage for.
 *
 * @param domainSettings where the type is fetched from and how many ids fit in one request
 * @param resourceType   the JSON:API resource type (e.g. {@code "countries"})
 * @param ids            the resource ids to fetch
 * @param includes       relationship names for the downstream {@code include} parameter
 */
public record BatchFetch(DomainSettings domainSettings,
                         String resourceType,
                         Set<String> ids,
                         Set<String> includes) {

    /**
     * @return the same fetch narrowed to {@code ids} - a chunk, or the cache misses
     */
    public BatchFetch withIds(Set<String> ids) {
        return new BatchFetch(domainSettings, resourceType, ids, includes);
    }

}
