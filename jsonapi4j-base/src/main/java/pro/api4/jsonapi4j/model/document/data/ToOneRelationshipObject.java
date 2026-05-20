package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

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
