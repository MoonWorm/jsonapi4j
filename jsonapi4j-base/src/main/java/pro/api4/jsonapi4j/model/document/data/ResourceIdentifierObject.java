package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;

/**
 * JSON:API Specification reference:
 * <a href="https://jsonapi.org/format/#document-resource-identifier-objects">Resource Identifier Object</a>
 */
@Getter
@EqualsAndHashCode
@ToString
public class ResourceIdentifierObject {

    public static final String ID_FIELD = "id";
    public static final String TYPE_FIELD = "type";
    public static final String META_FIELD = "meta";
    public static final String LID_FIELD = "lid";

    private final String id;
    private final String type;
    private final Object meta;
    private final String lid;

    public ResourceIdentifierObject(String id,
                                    String lid,
                                    String type,
                                    Object meta) {
        this.id = id;
        this.lid = lid;
        this.type = type;
        this.meta = meta;
    }

}
