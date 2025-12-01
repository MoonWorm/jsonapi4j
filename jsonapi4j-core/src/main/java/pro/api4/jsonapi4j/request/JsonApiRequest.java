package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.operation.OperationType;

public interface JsonApiRequest extends
        ResourceAwareRequest,
        RelationshipAwareRequest,
        CursorAwareRequest,
        IncludeAwareRequest,
        FiltersAwareRequest,
        SortAwareRequest,
        CustomQueryParamsAwareRequest,
        PayloadAwareRequest {

    static JsonApiRequestBuilder builder() {
        return new JsonApiRequestBuilder();
    }

    static JsonApiRequest composeRelationshipRequest(String resourceId,
                                                     Relationship<?, ?> relationship) {
        return builder()
                .targetResourceType(relationship.resourceType())
                .resourceId(resourceId)
                .targetRelationship(relationship.relationshipName())
                .operationType(relationship instanceof ToOneRelationship<?, ?>
                        ? OperationType.READ_TO_ONE_RELATIONSHIP
                        : OperationType.READ_TO_MANY_RELATIONSHIP
                )
                .build();
    }

    static JsonApiRequest composeReadByIdRequest(String resourceId,
                                                 ResourceType targetResourceType) {
        return builder()
                .targetResourceType(targetResourceType)
                .resourceId(resourceId)
                .operationType(OperationType.READ_RESOURCE_BY_ID)
                .build();
    }

    /**
     * @return this operation {@link OperationType} that is basically represents one of the available JSON:API
     * operations.
     */
    OperationType getOperationType();

}
