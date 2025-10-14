package pro.api4.jsonapi4j.processor.single.resource;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;

@FunctionalInterface
public interface SingleResourceDocSupplier<
        RESOURCE extends ResourceObject<?, ?>,
        DOC extends SingleResourceDoc<RESOURCE>> {

    DOC get(RESOURCE data,
            LinksObject links,
            Object meta
    );

}
