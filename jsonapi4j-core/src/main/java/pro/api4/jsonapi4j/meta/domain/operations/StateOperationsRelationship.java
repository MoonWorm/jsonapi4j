package pro.api4.jsonapi4j.meta.domain.operations;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.ToManyRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;

@JsonApiRelationship(relationshipName = OperationsResource.OPERATIONS, parentResource = StateResource.class)
public class StateOperationsRelationship implements ToManyRefRelationship<ResourceRef> {
}
