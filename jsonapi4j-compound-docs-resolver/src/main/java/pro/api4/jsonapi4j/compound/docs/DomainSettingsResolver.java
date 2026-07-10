package pro.api4.jsonapi4j.compound.docs;

/**
 * SPI used by the compound documents resolver to look up per-domain settings — base URL plus the
 * maximum number of resource IDs that can be requested in a single downstream
 * {@code filter[id]=...} batch — for a given JSON:API resource type.
 *
 * <p>When the resolver needs more IDs than {@link DomainSettings#maxBatchSize()} for a type, it
 * splits the request into parallel chunks of that size.
 */
@FunctionalInterface
public interface DomainSettingsResolver {

    /**
     * Resolves the settings for {@code resourceType}. Implementations may use {@code selfBaseUrl} — the requesting
     * app's own JSON:API base URL derived from the incoming request (e.g. {@code https://host:port/ctx/jsonapi}) — to
     * default unmapped, same-app resource types (notably the built-in meta types) to the very endpoint the request
     * arrived on, so no {@code jsonapi4j.cd.mapping.*} entry and no configured base URL are required for them.
     *
     * @param resourceType the JSON:API resource type to resolve
     * @param selfBaseUrl  the requesting app's own JSON:API root derived from the incoming request, or {@code null}
     *                     when unavailable
     * @return the resolved settings for {@code resourceType}
     */
    DomainSettings resolveDomainSettings(String resourceType, String selfBaseUrl);

}
