package pro.api4.jsonapi4j.model.document.data;


import lombok.EqualsAndHashCode;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.BaseDoc;
import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

@EqualsAndHashCode(of = {"data"}, callSuper = false)
@ToString(callSuper = true)
public abstract class AbstractSingleDataItemDoc<DATA_ITEM extends ResourceIdentifierObject> extends BaseDoc {

    public static final String DATA_FIELD = "data";
    public static final String INCLUDED_FIELD = "included";

    private final DATA_ITEM data;
    private List<? extends ResourceObject<?, ?>> included;

    public AbstractSingleDataItemDoc(DATA_ITEM data,
                                     LinksObject links,
                                     Object meta,
                                     List<? extends ResourceObject<?, ?>> included,
                                     JsonApiObject jsonapi) {
        super(links, meta, jsonapi);
        this.data = data;
        this.included = included;
    }

    public DATA_ITEM getData() {
        return data;
    }

    public List<? extends ResourceObject<?, ?>> getIncluded() {
        return included;
    }
}
