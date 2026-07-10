package pro.api4.jsonapi4j.meta.operation.resources;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.resources.StateResourcesRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource.StateAttributes;
import pro.api4.jsonapi4j.operation.ToManyRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

@JsonApiRelationshipOperation(relationship = StateResourcesRelationship.class)
public class StateResourcesRelationshipOperations implements ToManyRelationshipOperations<StateAttributes, Ref> {

    private final ResourcesIntrospector introspector;

    public StateResourcesRelationshipOperations(ResourcesIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public PaginationAwareResponse<Ref> readMany(JsonApiRequest relationshipRequest) {
        return PaginationAwareResponse.fromItemsNotPageable(introspector.resourceRefs());
    }

    @Override
    public PaginationAwareResponse<Ref> readManyForResource(JsonApiRequest relationshipRequest,
                                                            StateAttributes a) {
        return PaginationAwareResponse.fromItemsNotPageable(introspector.resourceRefs());
    }

}
