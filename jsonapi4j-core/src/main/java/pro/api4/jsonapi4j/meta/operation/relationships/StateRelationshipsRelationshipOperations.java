package pro.api4.jsonapi4j.meta.operation.relationships;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.relationships.StateRelationshipsRelationship;
import pro.api4.jsonapi4j.meta.domain.state.StateResource.StateAttributes;
import pro.api4.jsonapi4j.operation.ToManyRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

@JsonApiRelationshipOperation(relationship = StateRelationshipsRelationship.class)
public class StateRelationshipsRelationshipOperations implements ToManyRelationshipOperations<StateAttributes, Ref> {

    private final RelationshipsIntrospector introspector;

    public StateRelationshipsRelationshipOperations(RelationshipsIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public PaginationAwareResponse<Ref> readMany(JsonApiRequest relationshipRequest) {
        return PaginationAwareResponse.fromItemsNotPageable(introspector.relationshipRefs());
    }

    @Override
    public PaginationAwareResponse<Ref> readManyForResource(JsonApiRequest relationshipRequest,
                                                            StateAttributes a) {
        return PaginationAwareResponse.fromItemsNotPageable(introspector.relationshipRefs());
    }

}
