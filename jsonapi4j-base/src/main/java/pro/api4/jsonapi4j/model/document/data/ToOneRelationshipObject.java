package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@Getter
public class ToOneRelationshipObject extends RelationshipObject {

    public static final String DATA_FIELD = "data";

    private final ResourceIdentifierObject data;

    public ToOneRelationshipObject(ResourceIdentifierObject data,
                                   LinksObject links,
                                   Object meta) {
        super(links, meta);
        this.data = data;
    }

    public ToOneRelationshipObject(ResourceIdentifierObject data,
                                   LinksObject links) {
        this(data, links, null);
    }

    public ToOneRelationshipObject(LinksObject links) {
        this(null, links, null);
    }

    public ToOneRelationshipObject(ResourceIdentifierObject data) {
        this(data, null);
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
