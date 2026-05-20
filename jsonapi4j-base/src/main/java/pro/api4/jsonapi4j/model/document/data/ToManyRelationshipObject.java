package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

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
