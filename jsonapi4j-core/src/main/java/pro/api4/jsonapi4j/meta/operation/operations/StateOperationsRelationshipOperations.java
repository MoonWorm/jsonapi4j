package pro.api4.jsonapi4j.meta.operation.operations;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.operations.StateOperationsRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource.StateAttributes;
import pro.api4.jsonapi4j.operation.ToManyRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

@JsonApiRelationshipOperation(relationship = StateOperationsRelationship.class)
public class StateOperationsRelationshipOperations implements ToManyRelationshipOperations<StateAttributes, Ref> {

    private final OperationsIntrospector introspector;

    public StateOperationsRelationshipOperations(OperationsIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public PaginationAwareResponse<Ref> readMany(JsonApiRequest relationshipRequest) {
        return PaginationAwareResponse.fromItemsNotPageable(introspector.operationRefs());
    }

    @Override
    public PaginationAwareResponse<Ref> readManyForResource(JsonApiRequest relationshipRequest,
                                                            StateAttributes a) {
        return PaginationAwareResponse.fromItemsNotPageable(introspector.operationRefs());
    }

}
