package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

/**
 * Spec ref: <a href="https://jsonapi.org/format/#document-resource-objects">Resource Object</a>
 */
@Getter
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class ResourceObject<A, R> extends ResourceIdentifierObject {

    public static final String ATTRIBUTES_FIELD = "attributes";
    public static final String RELATIONSHIPS_FIELD = "relationships";
    public static final String LINKS_FIELD = "links";

    private final A attributes;
    private final R relationships;
    private final LinksObject links;

    public ResourceObject(String id,
                          String lid,
                          String type,
                          A attributes,
                          R relationships,
                          LinksObject links,
                          Object meta) {
        super(id, lid, type, meta);
        this.attributes = attributes;
        this.relationships = relationships;
        this.links = links;
    }

}
