package io.jsonapi4j.plugin;

import io.jsonapi4j.operation.RelationshipOperation;
import io.jsonapi4j.operation.ResourceOperation;

import java.util.Collections;
import java.util.List;

/**
 * Allows to attach as many custom {@link OperationPlugin} to {@link ResourceOperation} or
 * {@link RelationshipOperation} as needed.
 * <p>
 * There are some plugins exist as part of jsonapi4j framework that are sort of optional addition to the core
 * functionality, for example <a href="https://swagger.io/specification/">Open API</a>
 * {@link io.jsonapi4j.operation.plugin.OperationOasPlugin} plugin.
 * <p>
 * These plugins carry some extra static information (declarative approach) that is needed for various
 * customizations, e.g. declare Access Control rules for a particular relationship or tune some default captions for OAS.
 */
public interface OperationPluginAware {

    default <P extends OperationPlugin<P>> P getPlugin(Class<P> pluginClass) {
        return getPluginOrDefault(pluginClass, null);
    }

    default <P extends OperationPlugin<P>> P getPluginOrDefault(Class<P> pluginClass,
                                                                P defaultPlugin) {
        return plugins().stream()
                .filter(p -> p.getPluginClass().isAssignableFrom(pluginClass))
                .findFirst()
                .map(pluginClass::cast)
                .orElse(defaultPlugin);
    }

    default List<OperationPlugin<?>> plugins() {
        return Collections.emptyList();
    }

}
