package io.jsonapi4j.processor.single.relationship;

import io.jsonapi4j.model.document.LinksObject;
import io.jsonapi4j.model.document.data.ResourceIdentifierObject;
import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;

@FunctionalInterface
public interface ToOneRelationshipDocSupplier<RESPONSE extends ToOneRelationshipDoc> {

    RESPONSE get(
            ResourceIdentifierObject data,
            LinksObject links,
            Object meta
    );

}
