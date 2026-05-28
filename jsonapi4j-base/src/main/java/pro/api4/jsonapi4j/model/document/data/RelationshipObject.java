package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

/**
 * Base class for JSON:API relationship objects.
 *
 * <p>Represents the {@code "relationships"} member of a resource object as defined by the
 * <a href="https://jsonapi.org/format/#document-resource-object-relationships">JSON:API specification</a>.
 * Contains optional {@code "links"} and {@code "meta"} members; the {@code "data"} member is
 * provided by concrete subclasses {@link ToOneRelationshipObject} and {@link ToManyRelationshipObject}.
 *
 * @see ToOneRelationshipObject
 * @see ToManyRelationshipObject
 */
@Getter
@ToString
@EqualsAndHashCode
public class RelationshipObject {

    public static final String DATA_FIELD = "data";
    public static final String LINKS_FIELD = "links";
    public static final String META_FIELD = "meta";

    private final LinksObject links;
    private final Object meta;

    public RelationshipObject(LinksObject links,
                              Object meta) {
        this.links = links;
        this.meta = meta;
    }

    public RelationshipObject(LinksObject links) {
        this(links, null);
    }

}
