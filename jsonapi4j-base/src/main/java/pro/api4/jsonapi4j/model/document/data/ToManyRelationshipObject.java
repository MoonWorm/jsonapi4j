package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

/**
 * JSON:API to-many relationship object containing a list of {@link ResourceIdentifierObject}s
 * as its {@code "data"} member.
 *
 * <p>Represents relationships where a resource is linked to zero or more other resources,
 * e.g. {@code "comments"} on an article.
 *
 * @see RelationshipObject
 * @see ToOneRelationshipObject
 * @see <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API Relationships</a>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ToManyRelationshipObject extends RelationshipObject {

    private final List<ResourceIdentifierObject> data;

    public ToManyRelationshipObject(List<ResourceIdentifierObject> data,
                                    LinksObject links,
                                    Object meta) {
        super(links, meta);
        this.data = data;
    }


    public static ToManyRelationshipObject fromRelationshipObject(List<ResourceIdentifierObject> data,
                                                                  RelationshipObject relationshipObject) {
        return new ToManyRelationshipObject(
                data,
                relationshipObject.getLinks(),
                relationshipObject.getMeta()
        );
    }

}
