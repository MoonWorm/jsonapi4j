package pro.api4.jsonapi4j.domain;

import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;

/**
 * Default {@link ResourceRef} implementation that carries nothing but the referenced resource's
 * {@code "type"} and {@code "id"}.
 *
 * @param type the JSON:API {@code "type"} of the referenced resource, e.g. {@code "countries"}
 * @param id   the JSON:API {@code "id"} of the referenced resource, e.g. {@code "NO"}
 */
public record DefaultResourceRef(String type, String id) implements ResourceRef {

    /**
     * Creates a reference to the given resource.
     * <p>
     * Returns the {@link ResourceRef} interface type so that streams infer {@code List<ResourceRef>},
     * e.g. {@code ids.stream().map(id -> DefaultResourceRef.of("countries", id)).toList()}.
     *
     * @param type the JSON:API {@code "type"} of the referenced resource, e.g. {@code "countries"}
     * @param id   the JSON:API {@code "id"} of the referenced resource, e.g. {@code "NO"}
     * @return a {@link DefaultResourceRef} carrying the given {@code "type"} and {@code "id"}
     */
    public static ResourceRef of(String type, String id) {
        return new DefaultResourceRef(type, id);
    }

    /**
     * Creates a reference from a resource identifier received in a request payload.
     *
     * @param resourceIdentifier the resource identifier object, e.g. an element of a relationship's {@code "data"}
     * @return a {@link DefaultResourceRef} carrying the same {@code "type"} and {@code "id"}
     */
    public static ResourceRef from(ResourceIdentifierObject resourceIdentifier) {
        return of(resourceIdentifier.getType(), resourceIdentifier.getId());
    }

}
