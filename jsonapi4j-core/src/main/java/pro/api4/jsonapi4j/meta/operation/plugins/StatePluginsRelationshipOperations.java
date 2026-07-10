package pro.api4.jsonapi4j.meta.operation.plugins;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.plugins.StatePluginsRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource.StateAttributes;
import pro.api4.jsonapi4j.operation.ToManyRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

@JsonApiRelationshipOperation(relationship = StatePluginsRelationship.class)
public class StatePluginsRelationshipOperations implements ToManyRelationshipOperations<StateAttributes, Ref> {

    private final PluginsIntrospector introspector;

    public StatePluginsRelationshipOperations(PluginsIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public PaginationAwareResponse<Ref> readMany(JsonApiRequest relationshipRequest) {
        return PaginationAwareResponse.fromItemsNotPageable(introspector.pluginRefs());
    }

    @Override
    public PaginationAwareResponse<Ref> readManyForResource(JsonApiRequest relationshipRequest,
                                                            StateAttributes a) {
        return PaginationAwareResponse.fromItemsNotPageable(introspector.pluginRefs());
    }

}
