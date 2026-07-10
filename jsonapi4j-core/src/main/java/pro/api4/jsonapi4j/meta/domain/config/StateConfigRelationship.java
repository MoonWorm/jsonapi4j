package pro.api4.jsonapi4j.meta.domain.config;

import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;

@JsonApiRelationship(relationshipName = ConfigResource.CONFIG, parentResource = StateResource.class)
public class StateConfigRelationship implements ToOneRelationship<Ref> {

    @Override
    public String resolveResourceIdentifierType(Ref ref) {
        return ref.type();
    }

    @Override
    public String resolveResourceIdentifierId(Ref ref) {
        return ref.id();
    }

}
