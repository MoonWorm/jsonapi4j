package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

/**
 * JSON:API to-one relationship object containing a single {@link ResourceIdentifierObject} as its
 * {@code "data"} member.
 *
 * <p>Represents relationships where a resource is linked to at most one other resource,
 * e.g. {@code "author"} on an article.
 *
 * @see RelationshipObject
 * @see ToManyRelationshipObject
 * @see <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationships</a>
 */
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public class ToOneRelationshipObject extends RelationshipObject {

    private final ResourceIdentifierObject data;

    public ToOneRelationshipObject(ResourceIdentifierObject data,
                                   LinksObject links,
                                   Object meta) {
        super(links, meta);
        this.data = data;
    }


    public static ToOneRelationshipObject fromRelationshipObject(ResourceIdentifierObject data,
                                                                 RelationshipObject relationshipObject) {
        return new ToOneRelationshipObject(
                data,
                relationshipObject.getLinks(),
                relationshipObject.getMeta()
        );
    }

}
