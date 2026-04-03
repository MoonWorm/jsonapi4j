package pro.api4.jsonapi4j.model.document;

import lombok.*;
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
@AllArgsConstructor
public class LinksObject {

    public static final String SELF_FIELD = "self";
    public static final String RELATED_FIELD = "related";
    public static final String NEXT_FIELD = "next";

    private String self;
    private Object related;
    private String next;

}
