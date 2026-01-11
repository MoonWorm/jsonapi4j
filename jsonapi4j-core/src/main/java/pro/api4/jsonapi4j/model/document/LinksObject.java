package pro.api4.jsonapi4j.model.document;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

/**
 * JSON:API specification reference:
 * <a href="https://jsonapi.org/format/#auto-id--link-objects">Links Object</a>
 */
@Getter
@Setter
@EqualsAndHashCode
@ToString
@SuperBuilder
public class LinksObject {

    public static final String SELF_FIELD = "self";
    public static final String RELATED_FIELD = "related";
    public static final String NEXT_FIELD = "next";

    private String self;
    private Object related;
    private String next;

    public LinksObject(String self,
                       Object related,
                       String next) {
        this.self = self;
        this.related = related;
        this.next = next;
    }

}
