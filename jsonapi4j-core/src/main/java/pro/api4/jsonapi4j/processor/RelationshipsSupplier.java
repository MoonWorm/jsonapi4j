package pro.api4.jsonapi4j.processor;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;

import java.util.Map;

@FunctionalInterface
public interface RelationshipsSupplier<RELATIONSHIPS> {

    RELATIONSHIPS get(
            Map<RelationshipName, ToManyRelationshipObject> toManyRelationshipsMap,
            Map<RelationshipName, ToOneRelationshipObject> toOneRelationshipMap
    );

}