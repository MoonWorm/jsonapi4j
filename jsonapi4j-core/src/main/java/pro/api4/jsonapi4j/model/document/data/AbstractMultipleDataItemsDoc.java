package pro.api4.jsonapi4j.model.document.data;

import pro.api4.jsonapi4j.model.document.LinksObject;
import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.List;

@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public abstract class AbstractMultipleDataItemsDoc<DATA_ITEM extends ResourceIdentifierObject> extends BaseDoc {

    private final List<DATA_ITEM> data;

    public AbstractMultipleDataItemsDoc(List<DATA_ITEM> data,
                                        Object meta,
                                        LinksObject links) {
        super(links, meta);
        this.data = data;
    }

    public List<DATA_ITEM> getData() {
        return data;
    }

}
