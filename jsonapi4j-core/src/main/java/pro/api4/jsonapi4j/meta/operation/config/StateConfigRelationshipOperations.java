package pro.api4.jsonapi4j.meta.operation.config;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.config.StateConfigRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource.StateAttributes;
import pro.api4.jsonapi4j.operation.ToOneRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;

@JsonApiRelationshipOperation(relationship = StateConfigRelationship.class)
public class StateConfigRelationshipOperations implements ToOneRelationshipOperations<StateAttributes, Ref> {

    private final ConfigIntrospector introspector;

    public StateConfigRelationshipOperations(ConfigIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public Ref readOne(JsonApiRequest relationshipRequest) {
        return introspector.configRef();
    }

    @Override
    public Ref readOneForResource(JsonApiRequest relationshipRequest, StateAttributes a) {
        return introspector.configRef();
    }

}
