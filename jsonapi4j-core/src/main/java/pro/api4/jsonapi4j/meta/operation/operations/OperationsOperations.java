package pro.api4.jsonapi4j.meta.operation.operations;

import org.apache.commons.collections4.CollectionUtils;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.meta.domain.operations.OperationsResource;
import pro.api4.jsonapi4j.meta.domain.operations.OperationsResource.OperationDescriptorAttributes;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.List;

@JsonApiResourceOperation(resource = OperationsResource.class)
public class OperationsOperations implements ResourceOperations<OperationDescriptorAttributes> {

    private final OperationsIntrospector introspector;

    public OperationsOperations(OperationsIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public OperationDescriptorAttributes readById(JsonApiRequest request) {
        return introspector.operationById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        request.getResourceId(), new ResourceType(OperationsResource.OPERATIONS))
                );
    }

    @Override
    public PaginationAwareResponse<OperationDescriptorAttributes> readPage(JsonApiRequest request) {
        List<OperationDescriptorAttributes> items = introspector.operations();
        List<String> ids = request.getFilters().get(ID_FILTER_NAME);
        if (CollectionUtils.isNotEmpty(ids)) {
            items = items.stream()
                    .filter(o -> ids.contains(OperationsResource.operationId(o)))
                    .toList();
        }
        return PaginationAwareResponse.fromItemsNotPageable(items);
    }

}
