package pro.api4.jsonapi4j.meta.operation.relationships;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.meta.domain.relationships.RelationshipsResource;
import pro.api4.jsonapi4j.meta.domain.relationships.RelationshipsResource.RelationshipDescriptorAttributes;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.List;

@JsonApiResourceOperation(resource = RelationshipsResource.class)
public class RelationshipsOperations implements ResourceOperations<RelationshipDescriptorAttributes> {

    private final RelationshipsIntrospector introspector;

    public RelationshipsOperations(RelationshipsIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public RelationshipDescriptorAttributes readById(JsonApiRequest request) {
        return introspector.relationshipById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        request.getResourceId(), new ResourceType(RelationshipsResource.RELATIONSHIPS)));
    }

    @Override
    public PaginationAwareResponse<RelationshipDescriptorAttributes> readPage(JsonApiRequest request) {
        List<RelationshipDescriptorAttributes> items = introspector.relationships();
        List<String> ids = request.getFilters().get(ID_FILTER_NAME);
        if (ids != null && !ids.isEmpty()) {
            items = items.stream()
                    .filter(r -> ids.contains(RelationshipsResource.relationshipId(r)))
                    .toList();
        }
        return PaginationAwareResponse.fromItemsNotPageable(items);
    }

}
