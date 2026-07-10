package pro.api4.jsonapi4j.meta.operation.resources;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.resources.ResourcesResource.ResourceDescriptorAttributes;

import java.util.List;
import java.util.Optional;

public interface ResourcesIntrospector {

    List<ResourceDescriptorAttributes> resources();

    Optional<ResourceDescriptorAttributes> resourceById(String id);

    List<Ref> resourceRefs();

}
