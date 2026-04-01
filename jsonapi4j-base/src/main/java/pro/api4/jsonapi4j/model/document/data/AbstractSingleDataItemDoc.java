package pro.api4.jsonapi4j.model.document.data;


import lombok.EqualsAndHashCode;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

@EqualsAndHashCode(of = {"data"}, callSuper = false)
@ToString(callSuper = true)
public abstract class AbstractSingleDataItemDoc<DATA_ITEM extends ResourceIdentifierObject> extends BaseDoc {

    public static final String DATA_FIELD = "data";

    private final DATA_ITEM data;

    public AbstractSingleDataItemDoc(DATA_ITEM data,
                                     LinksObject links,
                                     Object meta,
                                     List<? extends ResourceObject<?, ?>> included) {
        super(links, meta, included);
        this.data = data;
    }

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
