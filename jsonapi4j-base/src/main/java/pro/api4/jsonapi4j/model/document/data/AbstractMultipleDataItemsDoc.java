package pro.api4.jsonapi4j.model.document.data;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import pro.api4.jsonapi4j.model.document.BaseDoc;
import pro.api4.jsonapi4j.model.document.LinksObject;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class AbstractMultipleDataItemsDoc<DATA_ITEM extends ResourceIdentifierObject> extends BaseDoc {

    public static final String DATA_FIELD = "data";
    public static final String INCLUDED_FIELD = "included";

    private final List<DATA_ITEM> data;
    private List<? extends ResourceObject<?, ?>> included;

    public AbstractMultipleDataItemsDoc(List<DATA_ITEM> data,
                                        LinksObject links,
                                        Object meta,
                                        List<? extends ResourceObject<?, ?>> included,
                                        JsonApiObject jsonapi) {
        super(links, meta, jsonapi);
        this.data = data;
        this.included = included;
    }

    public List<DATA_ITEM> getData() {
        return data;
    }

    public List<? extends ResourceObject<?, ?>> getIncluded() {
        return included;
    }
}
