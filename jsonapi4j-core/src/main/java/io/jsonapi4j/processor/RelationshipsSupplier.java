package io.jsonapi4j.processor;

import io.jsonapi4j.domain.RelationshipName;
import io.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import io.jsonapi4j.model.document.data.ToOneRelationshipDoc;

import java.util.Map;

@FunctionalInterface
public interface RelationshipsSupplier<RELATIONSHIPS> {

    RELATIONSHIPS get(
            Map<RelationshipName, ToManyRelationshipsDoc> toManyRelationshipsMap,
            Map<RelationshipName, ToOneRelationshipDoc> toOneRelationshipMap
    );

}