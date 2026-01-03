package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.List;
import java.util.Map;

public interface ToManyRelationshipBatchAwareRepository<RESOURCE_DTO, RELATIONSHIP_DTO> extends
        ToManyRelationshipRepository<RESOURCE_DTO, RELATIONSHIP_DTO>, BatchReadToManyRelationshipOperation<RESOURCE_DTO, RELATIONSHIP_DTO> {

    @Override
    default Map<RESOURCE_DTO, CursorPageableResponse<RELATIONSHIP_DTO>> readBatches(JsonApiRequest request,
                                                                                    List<RESOURCE_DTO> resourceDtos) {
        throw new OperationNotFoundException(
                OperationType.READ_TO_MANY_RELATIONSHIP,
                request.getTargetResourceType(),
                request.getTargetRelationshipName()
        );
    }

}
