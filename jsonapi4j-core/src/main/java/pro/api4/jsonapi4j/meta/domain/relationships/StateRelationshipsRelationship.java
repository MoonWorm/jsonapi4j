package pro.api4.jsonapi4j.meta.domain.relationships;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.ToManyRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;

@JsonApiRelationship(relationshipName = RelationshipsResource.RELATIONSHIPS, parentResource = StateResource.class)
public class StateRelationshipsRelationship implements ToManyRefRelationship<ResourceRef> {
}
