package pro.api4.jsonapi4j.processor.resolvers.links;

import pro.api4.jsonapi4j.domain.ResourceType;

@FunctionalInterface
public interface ResourceTypeSupplier<T> {

    ResourceType getResourceType(T t);

}
