package pro.api4.jsonapi4j.meta.domain.config;

import pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;

import java.util.Map;

@JsonApiResource(resourceType = ConfigResource.CONFIG)
public class ConfigResource implements Resource<ConfigResource.ConfigAttributes> {

    public static final String CONFIG = "config";

    @Override
    public String resolveResourceId(ConfigAttributes a) {
        return MetaDomain.SINGLETON_ID;
    }

    @Override
    public Object resolveAttributes(ConfigAttributes a) {
        return a;
    }

    public record ConfigAttributes(Map<String, Object> settings) {}
}
