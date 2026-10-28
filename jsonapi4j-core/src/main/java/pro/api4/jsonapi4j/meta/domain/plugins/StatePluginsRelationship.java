package pro.api4.jsonapi4j.meta.domain.plugins;

import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.ToManyRefRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;

@JsonApiRelationship(relationshipName = PluginsResource.PLUGINS, parentResource = StateResource.class)
public class StatePluginsRelationship implements ToManyRefRelationship<ResourceRef> {
}
