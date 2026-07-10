package pro.api4.jsonapi4j.meta.domain.plugins;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;

@JsonApiResource(resourceType = PluginsResource.PLUGINS)
public class PluginsResource implements Resource<PluginsResource.PluginAttributes> {

    public static final String PLUGINS = "plugins";

    @Override
    public String resolveResourceId(PluginAttributes a) {
        return pluginId(a);
    }

    @Override
    public Object resolveAttributes(PluginAttributes a) {
        return a;
    }

    public record PluginAttributes(String name, boolean enabled, int precedence, String className) {
    }

    public static String pluginId(PluginAttributes a) {
        return a.name();
    }

}
