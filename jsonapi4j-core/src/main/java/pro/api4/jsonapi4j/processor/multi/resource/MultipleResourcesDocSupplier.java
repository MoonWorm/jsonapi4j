package pro.api4.jsonapi4j.processor.multi.resource;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;

import java.util.List;

@FunctionalInterface
public interface MultipleResourcesDocSupplier<
        RESOURCE extends ResourceObject<?, ?>,
        DOC extends MultipleResourcesDoc<RESOURCE>> {

    DOC get(List<RESOURCE> data,
            LinksObject links,
            Object meta);

}
