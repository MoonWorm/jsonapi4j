package pro.api4.jsonapi4j.processor.single.relationship;

import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;

@FunctionalInterface
public interface ToOneRelationshipDocSupplier<RESPONSE extends ToOneRelationshipDoc> {

    RESPONSE get(
            ResourceIdentifierObject data,
            LinksObject links,
            Object meta
    );

}
