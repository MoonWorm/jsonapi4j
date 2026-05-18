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

    DomainSettings resolveDomainSettings(String resourceType);

}
