package io.jsonapi4j.processor.multi.relationship;

import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import io.jsonapi4j.model.document.data.ResourceIdentifierObject;

import java.util.List;

@FunctionalInterface
public interface ToManyRelationshipsDocSupplier<DOC extends ToManyRelationshipsDoc> {

    DOC get(List<ResourceIdentifierObject> data,
            LinksObject links,
            Object meta);

}
