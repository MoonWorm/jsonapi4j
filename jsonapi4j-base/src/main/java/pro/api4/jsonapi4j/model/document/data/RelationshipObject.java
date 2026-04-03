package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

@Getter
@ToString
@EqualsAndHashCode
public class RelationshipObject {

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
