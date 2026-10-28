package pro.api4.jsonapi4j.meta.domain.resources;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.ToManyRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;

@JsonApiRelationship(relationshipName = ResourcesResource.RESOURCES, parentResource = StateResource.class)
public class StateResourcesRelationship implements ToManyRefRelationship<ResourceRef> {
}
