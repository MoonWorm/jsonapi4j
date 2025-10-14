package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;

@FunctionalInterface
public interface ResourceSupplier<ATTRIBUTES, RELATIONSHIPS, RESOURCE extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>> {

    RESOURCE get(String id,
                 String type,
                 ATTRIBUTES attributes,
                 RELATIONSHIPS relationships,
                 LinksObject links,
                 Object meta);

}