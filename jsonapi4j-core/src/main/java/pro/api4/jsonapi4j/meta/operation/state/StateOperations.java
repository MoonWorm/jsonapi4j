package pro.api4.jsonapi4j.meta.operation.state;

import pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;
import pro.api4.jsonapi4j.meta.domain.state.StateResource.StateAttributes;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.List;

@JsonApiResourceOperation(resource = StateResource.class)
public class StateOperations implements ResourceOperations<StateAttributes>, ReadMultipleResourcesOperation<StateAttributes> {

    private final StateIntrospector introspector;

    public StateOperations(StateIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public StateAttributes readById(JsonApiRequest request) {
        if (!MetaDomain.SINGLETON_ID.equals(request.getResourceId())) {
            throw new ResourceNotFoundException(request.getResourceId(), new ResourceType(StateResource.STATE));
        }
        return introspector.state();
    }

    @Override
    public PaginationAwareResponse<StateAttributes> readPage(JsonApiRequest request) {
        return PaginationAwareResponse.fromItemsNotPageable(List.of(introspector.state()));
    }

}
