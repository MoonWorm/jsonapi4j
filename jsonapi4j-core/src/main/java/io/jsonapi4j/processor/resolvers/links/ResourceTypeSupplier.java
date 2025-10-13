package io.jsonapi4j.processor.resolvers.links;

import io.jsonapi4j.domain.ResourceType;

@FunctionalInterface
public interface ResourceTypeSupplier<T> {

    ResourceType getResourceType(T t);

}
