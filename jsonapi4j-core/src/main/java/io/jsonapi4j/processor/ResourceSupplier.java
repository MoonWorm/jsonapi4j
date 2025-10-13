package io.jsonapi4j.processor;

import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.ResourceObject;

@FunctionalInterface
public interface ResourceSupplier<ATTRIBUTES, RELATIONSHIPS, RESOURCE extends ResourceObject<ATTRIBUTES, RELATIONSHIPS>> {

    RESOURCE get(String id,
                 String type,
                 ATTRIBUTES attributes,
                 RELATIONSHIPS relationships,
                 LinksObject links,
                 Object meta);

}