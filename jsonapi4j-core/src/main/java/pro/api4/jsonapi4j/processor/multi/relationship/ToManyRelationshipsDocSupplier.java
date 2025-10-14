package pro.api4.jsonapi4j.processor.multi.relationship;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;

import java.util.List;

@FunctionalInterface
public interface ToManyRelationshipsDocSupplier<DOC extends ToManyRelationshipsDoc> {

    DOC get(List<ResourceIdentifierObject> data,
            LinksObject links,
            Object meta);

}
