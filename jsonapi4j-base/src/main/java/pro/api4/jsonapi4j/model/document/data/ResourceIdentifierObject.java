package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.beans.ConstructorProperties;

/**
 * JSON:API Specification reference:
 * <a href="https://jsonapi.org/format/#document-resource-identifier-objects">Resource Identifier Object</a>
 */
@EqualsAndHashCode(of = {"id", "lid", "type"})
@ToString(of = {"id", "lid", "type"})
public class ResourceIdentifierObject {

    public static final String ID_FIELD = "id";
    public static final String LID_FIELD = "lid";
    public static final String TYPE_FIELD = "type";
    public static final String META_FIELD = "meta";

    private final String id;
    private final String lid;
    private final String type;
    private Object meta;

    @ConstructorProperties({ID_FIELD, LID_FIELD, TYPE_FIELD, META_FIELD})
    public ResourceIdentifierObject(String id,
                                    String lid,
                                    String type,
                                    Object meta) {
        this.id = id;
        this.lid = lid;
        this.type = type;
        this.meta = meta;
    }

    public ResourceIdentifierObject(String id,
                                    String type,
                                    Object meta) {
        this(id, null, type, meta);
    }

    public ResourceIdentifierObject(String id,
                                    String type) {
        this(id, type, null);
    }

    public String getId() {
        return id;
    }

    public String getType() {
        return type;
    }

    public String getLid() {
        return lid;
    }

    public Object getMeta() {
        return meta;
    }

}
