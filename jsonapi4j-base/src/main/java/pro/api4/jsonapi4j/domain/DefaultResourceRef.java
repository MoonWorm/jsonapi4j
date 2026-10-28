package pro.api4.jsonapi4j.domain;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;

import java.util.List;

/**
 * Default {@link ResourceRef} implementation that carries nothing but the referenced resource's
 * {@code "type"} and {@code "id"}.
 *
 * @param type the JSON:API {@code "type"} of the referenced resource, e.g. {@code "countries"}
 * @param id   the JSON:API {@code "id"} of the referenced resource, e.g. {@code "NO"}
 */
public record DefaultResourceRef(String type, String id) implements ResourceRef {

    /**
     * @throws NullPointerException if {@code type} or {@code id} is {@code null} - a resource identifier
     *                              requires both
     */
    public DefaultResourceRef {
        Validate.notNull(type, "type must not be null");
        Validate.notNull(id, "id must not be null");
    }

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

    /**
     * Creates a reference from a to-one relationship received in a request payload, e.g.
     * {@code request.getToOneRelationshipDocPayload()} or a to-one relationship of a resource body.
     *
     * @param relationship the to-one relationship object
     * @return a reference carrying the same {@code "type"} and {@code "id"} as its {@code "data"}, or {@code null}
     * when {@code "data"} is {@code null}, i.e. the relationship is being cleared
     */
    public static ResourceRef from(ToOneRelationshipObject relationship) {
        Validate.notNull(relationship, "relationship must not be null");
        return relationship.getData() == null ? null : from(relationship.getData());
    }

    /**
     * Creates references from a to-many relationship received in a request payload, e.g.
     * {@code request.getToManyRelationshipDocPayload()} or a to-many relationship of a resource body.
     *
     * @param relationship the to-many relationship object
     * @return references carrying the same {@code "type"} and {@code "id"} as the elements of its
     * {@code "data"}, in the same order; empty when {@code "data"} is {@code null} or empty
     */
    public static List<ResourceRef> listFrom(ToManyRelationshipObject relationship) {
        Validate.notNull(relationship, "relationship must not be null");
        return ListUtils.emptyIfNull(relationship.getData())
                .stream()
                .map(DefaultResourceRef::from)
                .toList();
    }

}
