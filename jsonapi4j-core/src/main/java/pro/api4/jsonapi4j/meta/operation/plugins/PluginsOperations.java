package pro.api4.jsonapi4j.meta.operation.plugins;

import org.apache.commons.collections4.CollectionUtils;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource;
import pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource.PluginAttributes;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.List;

import static pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource.pluginId;

@JsonApiResourceOperation(resource = PluginsResource.class)
public class PluginsOperations implements ResourceOperations<PluginAttributes>, ReadMultipleResourcesOperation<PluginAttributes> {

    private final PluginsIntrospector introspector;

    public PluginsOperations(PluginsIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public PluginAttributes readById(JsonApiRequest request) {
        return introspector.pluginById(request.getResourceId())
                .orElseThrow(() -> new ResourceNotFoundException(
                        request.getResourceId(), new ResourceType(PluginsResource.PLUGINS)));
    }

    @Override
    public PaginationAwareResponse<PluginAttributes> readPage(JsonApiRequest request) {
        List<PluginAttributes> items = introspector.plugins();
        List<String> ids = request.getFilters().get(ID_FILTER_NAME);
        if (CollectionUtils.isNotEmpty(ids)) {
            items = items.stream().filter(p -> ids.contains(pluginId(p))).toList();
        }
        return PaginationAwareResponse.fromItemsNotPageable(items);
    }

}
