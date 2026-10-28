package pro.api4.jsonapi4j.meta.domain.config;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.ToOneRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;

@JsonApiRelationship(relationshipName = ConfigResource.CONFIG, parentResource = StateResource.class)
public class StateConfigRelationship implements ToOneRefRelationship<ResourceRef> {
}
