package pro.api4.jsonapi4j.meta.operation.resources;

import org.apache.commons.collections4.CollectionUtils;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.meta.domain.resources.ResourcesResource;
import pro.api4.jsonapi4j.meta.domain.resources.ResourcesResource.ResourceDescriptorAttributes;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.List;

import static pro.api4.jsonapi4j.meta.domain.resources.ResourcesResource.resourceId;

@JsonApiResourceOperation(resource = ResourcesResource.class)
public class ResourcesOperations implements ResourceOperations<ResourceDescriptorAttributes> {

    private final ResourcesIntrospector introspector;

    public ResourcesOperations(ResourcesIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public ResourceDescriptorAttributes readById(JsonApiRequest request) {
        return introspector.resourceById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        request.getResourceId(), new ResourceType(ResourcesResource.RESOURCES)));
    }

    @Override
    public PaginationAwareResponse<ResourceDescriptorAttributes> readPage(JsonApiRequest request) {
        List<ResourceDescriptorAttributes> items = introspector.resources();
        List<String> ids = request.getFilters().get(ID_FILTER_NAME);
        if (CollectionUtils.isNotEmpty(ids)) {
            items = items.stream().filter(r -> ids.contains(resourceId(r))).toList();
        }
        return PaginationAwareResponse.fromItemsNotPageable(items);
    }

}
