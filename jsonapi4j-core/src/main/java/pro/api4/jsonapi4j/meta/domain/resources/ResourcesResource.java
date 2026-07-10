package pro.api4.jsonapi4j.meta.domain.resources;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;

@JsonApiResource(resourceType = ResourcesResource.RESOURCES)
public class ResourcesResource implements Resource<ResourcesResource.ResourceDescriptorAttributes> {

    public static final String RESOURCES = "resources";

    public static String resourceId(ResourceDescriptorAttributes a) {
        return a.type();
    }

    @Override
    public String resolveResourceId(ResourceDescriptorAttributes a) {
        return resourceId(a);
    }

    @Override
    public Object resolveAttributes(ResourceDescriptorAttributes a) {
        return a;
    }

    public record ResourceDescriptorAttributes(String type, String className) {
    }

}
