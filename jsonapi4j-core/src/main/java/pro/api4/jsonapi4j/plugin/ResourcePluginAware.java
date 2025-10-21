package pro.api4.jsonapi4j.plugin;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.plugin.ac.ResourceOutboundAccessControlPlugin;
import pro.api4.jsonapi4j.domain.plugin.oas.ResourceOasPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Allows to attach as many custom {@link ResourcePlugin} to {@link Resource} as needed.
 * <p>
 * There are some plugins exist as part of jsonapi4j framework that are sort of optional addition to the core
 * functionality, for example various <a href="https://swagger.io/specification/">Open API</a>
 * {@link ResourceOasPlugin} and Access Control
 * {@link ResourceOutboundAccessControlPlugin} /
 * plugins.
 * <p>
 * These plugins carry some extra static information (declarative approach) that is needed for various
 * customizations, e.g. declare Access Control rules for a particular resource/attributes or specify attributes class
 * explicitly that will be used for a proper OAS generation.
 */
public interface ResourcePluginAware {

    default <P extends ResourcePlugin<P>> P getPlugin(Class<P> pluginClass) {
        return getPluginOrDefault(pluginClass, null);
    }

    default <P extends ResourcePlugin<P>> P getPluginOrDefault(Class<P> pluginClass,
                                                               P defaultPlugin) {
        return plugins().stream()
                .filter(p -> p.getPluginClass().isAssignableFrom(pluginClass))
                .findFirst()
                .map(pluginClass::cast)
                .orElse(defaultPlugin);
    }

    default List<ResourcePlugin<?>> plugins() {
        return Collections.emptyList();
    }

}
