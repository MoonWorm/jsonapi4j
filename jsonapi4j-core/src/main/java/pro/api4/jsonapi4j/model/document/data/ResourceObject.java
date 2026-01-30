package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

/**
 * Spec ref: <a href="https://jsonapi.org/format/#document-resource-objects">Resource Object</a>
 */
@ToString(callSuper = true, exclude = {"attributes", "relationships", "links"})
@EqualsAndHashCode(exclude = {"links"}, callSuper = true)
public class ResourceObject<A, R> extends ResourceIdentifierObject {

    public static final String ATTRIBUTES_FIELD = "attributes";
    public static final String RELATIONSHIPS_FIELD = "relationships";
    public static final String LINKS_FIELD = "links";

    private A attributes;
    private R relationships;
    private LinksObject links;

    public ResourceObject(String id,
                          String type,
                          A attributes,
                          R relationships,
                          LinksObject links,
                          Object meta) {
        super(id, type, meta);
        this.attributes = attributes;
        this.relationships = relationships;
        this.links = links;
    }

    public ResourceObject(String id,
                          String type,
                          A attributes,
                          R relationships,
                          LinksObject links) {
        this(id, type, attributes, relationships, links, null);
    }

    public ResourceObject(String id,
                          String type,
                          A attributes,
                          R relationships) {
        this(id, type, attributes, relationships, null);
    }

    public A getAttributes() {
        return attributes;
    }

    public R getRelationships() {
        return relationships;
    }

    public LinksObject getLinks() {
        return links;
    }

}
