package pro.api4.jsonapi4j.meta.operation.config;

import pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.meta.domain.config.ConfigResource;
import pro.api4.jsonapi4j.meta.domain.config.ConfigResource.ConfigAttributes;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;

import java.util.List;


@JsonApiResourceOperation(resource = ConfigResource.class)
public class ConfigOperations implements ResourceOperations<ConfigAttributes> {

    private final ConfigIntrospector introspector;

    public ConfigOperations(ConfigIntrospector introspector) {
        this.introspector = introspector;
    }

    @Override
    public ConfigAttributes readById(JsonApiRequest request) {
        if (!MetaDomain.SINGLETON_ID.equals(request.getResourceId())) {
            throw new ResourceNotFoundException(request.getResourceId(), new ResourceType(ConfigResource.CONFIG));
        }
        return introspector.config();
    }

    @Override
    public PaginationAwareResponse<ConfigAttributes> readPage(JsonApiRequest request) {
        return PaginationAwareResponse.fromItemsNotPageable(List.of(introspector.config()));
    }

}
