package pro.api4.jsonapi4j.request;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
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
                                                     ResourceType resourceType,
                                                     RelationshipName relationshipName,
                                                     OperationType operationType) {
        return builder()
                .targetResourceType(resourceType)
                .operationType(operationType)
                .resourceId(resourceId)
                .targetRelationship(relationshipName)
                .build();
    }

    static JsonApiRequest composeResourceRequest(String resourceId,
                                                 ResourceType resourceType,
                                                 OperationType operationType) {
        return builder()
                .targetResourceType(resourceType)
                .resourceId(resourceId)
                .operationType(operationType)
                .build();
    }

    /**
     * @return this operation {@link OperationType} that is basically represents one of the available JSON:API
     * operations.
     */
    OperationType getOperationType();

}
