package pro.api4.jsonapi4j.domain;

/**
 * Lightweight reference to a JSON:API resource - just its {@code "type"} and {@code "id"}.
 * <p>
 * A relationship only ever emits a
 * <a href="https://jsonapi.org/format/#document-resource-identifier-objects">Resource Identifier Object</a>,
 * so in most cases this is all a relationship DTO needs to carry. Use it together with
 * {@link ToOneRefRelationship} or {@link ToManyRefRelationship} to avoid implementing
 * {@code resolveResourceIdentifierType}/{@code resolveResourceIdentifierId} by hand.
 * <p>
 * {@link DefaultResourceRef} is the default implementation. Implement this interface in your own DTO (records
 * work well) when the reference needs to carry more, e.g. data for the identifier's {@code "meta"} member.
 * Equality is up to each implementation, so references of different implementation types are not equal even if
 * they point to the same resource.
 * <p>
 * The full target resource is materialized separately by its own {@link Resource} (and, on
 * {@code ?include=}, by the Compound Docs plugin).
 */
public interface ResourceRef {

    /**
     * @return the JSON:API {@code "type"} of the referenced resource, e.g. {@code "countries"}
     */
    String type();

    /**
     * @return the JSON:API {@code "id"} of the referenced resource, e.g. {@code "NO"}
     */
    String id();

}
