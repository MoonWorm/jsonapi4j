package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;

import java.util.Map;

@FunctionalInterface
public interface RelationshipsSupplier<RELATIONSHIPS> {

    RELATIONSHIPS get(
            Map<RelationshipName, ToManyRelationshipsDoc> toManyRelationshipsMap,
            Map<RelationshipName, ToOneRelationshipDoc> toOneRelationshipMap
    );

}