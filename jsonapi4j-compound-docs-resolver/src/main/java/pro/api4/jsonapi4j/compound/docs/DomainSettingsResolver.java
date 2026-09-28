package pro.api4.jsonapi4j.compound.docs;

import java.util.Optional;

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
     * Resolves where {@code resourceType} is fetched from.
     *
     * <p>An empty result means there is no route for the type. Used standalone (e.g. in an API gateway) that fails
     * resolution with an error naming the type. The CD plugin instead treats such a type as served by the app itself
     * and fetches it over loopback.
     *
     * @param resourceType the JSON:API resource type to resolve
     * @return the settings for {@code resourceType}, or empty when there is no route for it
     */
    Optional<DomainSettings> resolveDomainSettings(String resourceType);

}
