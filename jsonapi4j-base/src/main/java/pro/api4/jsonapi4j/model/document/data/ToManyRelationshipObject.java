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

    public static final String DATA_FIELD = "data";

    private final List<ResourceIdentifierObject> data;

    public ToManyRelationshipObject(List<ResourceIdentifierObject> data,
                                    LinksObject links,
                                    Object meta) {
        super(links, meta);
        this.data = data;
    }

    public ToManyRelationshipObject(List<ResourceIdentifierObject> data,
                                    LinksObject links) {
        this(data, links, null);
    }

    public ToManyRelationshipObject(LinksObject links) {
        this(null, links, null);
    }

    public ToManyRelationshipObject(List<ResourceIdentifierObject> data) {
        this(data, null);
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
