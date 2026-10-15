package pro.api4.jsonapi4j.compound.docs.client;

import pro.api4.jsonapi4j.compound.docs.Deadline;
import pro.api4.jsonapi4j.compound.docs.DomainSettings;

import java.util.Set;

/**
 * One fetch of resources of a single type, by id, with the relationships to request linkage for.
 *
 * @param domainSettings where the type is fetched from - which also fixes the kind of route, {@code R} - and how many
 *                       ids fit in one request
 * @param resourceType   the JSON:API resource type (e.g. {@code "countries"})
 * @param ids            the resource ids to fetch
 * @param includes       relationship names to request linkage for
 * @param deadline       by when the include resolution this fetch is part of must finish
 * @param <R>            the kind of route
 */
public record BatchFetch<R extends DomainSettings>(R domainSettings,
                                                   String resourceType,
                                                   Set<String> ids,
                                                   Set<String> includes,
                                                   Deadline deadline) {

    public BatchFetch {
        deadline = deadline == null ? Deadline.none() : deadline;
    }

    /**
     * A fetch with no deadline.
     */
    public BatchFetch(R domainSettings, String resourceType, Set<String> ids, Set<String> includes) {
        this(domainSettings, resourceType, ids, includes, Deadline.none());
    }

    /**
     * @return the same fetch narrowed to {@code ids} - a chunk, or the cache misses
     */
    public BatchFetch<R> withIds(Set<String> ids) {
        return new BatchFetch<>(domainSettings, resourceType, ids, includes, deadline);
    }

    /**
     * @return the same fetch, typed by the kind of its route - e.g. once {@code domainSettings} is known to be
     * {@link DomainSettings.InProcess}
     */
    public <S extends DomainSettings> BatchFetch<S> withDomainSettings(S domainSettings) {
        return new BatchFetch<>(domainSettings, resourceType, ids, includes, deadline);
    }

}
