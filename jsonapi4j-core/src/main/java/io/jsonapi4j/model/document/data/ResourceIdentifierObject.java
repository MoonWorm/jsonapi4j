package io.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.ToString;

/**
 * JSON:API Specification reference:
 * <a href="https://jsonapi.org/format/#document-resource-identifier-objects">Resource Identifier Object</a>
 */
@EqualsAndHashCode(of = {"id", "type"})
@ToString(of = {"id", "type"})
public class ResourceIdentifierObject {

    private final String id;
    private final String type;
    private Object meta;

    public ResourceIdentifierObject(String id,
                                    String type,
                                    Object meta) {
        this.id = id;
        this.type = type;
        this.meta = meta;
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

    public Object getMeta() {
        return meta;
    }

}
