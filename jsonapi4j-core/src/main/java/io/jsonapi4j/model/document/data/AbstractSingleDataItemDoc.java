package io.jsonapi4j.model.document.data;


import io.jsonapi4j.model.document.LinksObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(of = {"data"}, callSuper = false)
@ToString(callSuper = true)
public abstract class AbstractSingleDataItemDoc<DATA_ITEM extends ResourceIdentifierObject> extends BaseDoc {

    private final DATA_ITEM data;

    public AbstractSingleDataItemDoc(DATA_ITEM data,
                                     LinksObject links,
                                     Object meta) {
        super(links, meta);
        this.data = data;
    }

    public DATA_ITEM getData() {
        return data;
    }
}
