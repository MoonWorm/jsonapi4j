package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;
import java.util.Map;

public interface ToOneRelationshipBatchAwareRepository<RESOURCE_DTO, RELATIONSHIP_DTO> extends
        ToOneRelationshipRepository<RESOURCE_DTO, RELATIONSHIP_DTO>, BatchReadToOneRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> {

    default Map<RESOURCE_DTO, RELATIONSHIP_DTO> readBatches(JsonApiRequest request,
                                                            List<RESOURCE_DTO> resourceDtos) {
        throw new OperationNotFoundException(
                OperationType.READ_TO_ONE_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

}
