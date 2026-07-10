package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RegisteredResource;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;

import java.util.stream.Stream;

/**
 * Shared resource-type selection for the OAS customizers. Meta ("introspection") resources are internal and must
 * never be documented, so the customizers iterate only the host resource types that have operations configured.
 * The meta classification is owned by {@link DomainRegistry} (its natural home); this helper keeps the recurring
 * "types with operations, minus meta" filter in one place.
 */
public final class OasResourceTypes {

    private OasResourceTypes() {
    }

    /**
     * @return the resource types that have at least one operation configured, excluding the reserved meta types.
     */
    public static Stream<ResourceType> resourceTypesWithOperationsExcludingMeta(DomainRegistry domainRegistry,
                                                                                OperationsRegistry operationsRegistry) {
        return operationsRegistry.getResourceTypesWithAnyOperationConfigured().stream()
                .filter(resourceType -> !domainRegistry.getMetaResourceTypes().contains(resourceType));
    }

    /**
     *
     * @return all registered resource types except for Meta ones
     */
    public static Stream<RegisteredResource<Resource<?>>> registeredResourcesExcludingMeta(DomainRegistry domainRegistry) {
        return domainRegistry.getResources()
                .stream()
                .filter(resourceConfig -> !domainRegistry.getMetaResourceTypes()
                        .contains(resourceConfig.getResourceType())
                );
    }

}
