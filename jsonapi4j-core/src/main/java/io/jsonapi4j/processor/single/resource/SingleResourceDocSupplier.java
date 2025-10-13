package io.jsonapi4j.processor.single.resource;

import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.ResourceObject;
import io.jsonapi4j.model.document.data.SingleResourceDoc;

@FunctionalInterface
public interface SingleResourceDocSupplier<
        RESOURCE extends ResourceObject<?, ?>,
        DOC extends SingleResourceDoc<RESOURCE>> {

    DOC get(RESOURCE data,
            LinksObject links,
            Object meta
    );

}
