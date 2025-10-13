package io.jsonapi4j.processor.multi.resource;

import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.MultipleResourcesDoc;
import io.jsonapi4j.model.document.data.ResourceObject;

import java.util.List;

@FunctionalInterface
public interface MultipleResourcesDocSupplier<
        RESOURCE extends ResourceObject<?, ?>,
        DOC extends MultipleResourcesDoc<RESOURCE>> {

    DOC get(List<RESOURCE> data,
            LinksObject links,
            Object meta);

}
