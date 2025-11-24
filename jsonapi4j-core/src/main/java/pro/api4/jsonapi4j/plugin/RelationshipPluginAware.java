package pro.api4.jsonapi4j.plugin;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.plugin.oas.RelationshipOasPlugin;

import java.util.Collections;
import java.util.List;

/**
 * Allows to attach as many custom {@link RelationshipPlugin} to {@link Relationship} as needed.
 * <p>
 * There are some plugins exist as part of jsonapi4j framework that are sort of optional addition to the core
 * functionality, for example <a href="https://swagger.io/specification/">Open API</a>
 * {@link RelationshipOasPlugin}
 * plugin.
 * <p>
 * These plugins carry some extra static information (declarative approach) that is needed for various
 * customizations, e.g. declare Access Control rules for a particular relationship or tune some default captions for OAS.
 */
public interface RelationshipPluginAware {

    default <P extends RelationshipPlugin<P>> P getPlugin(Class<P> pluginClass) {
        return getPluginOrDefault(pluginClass, null);
    }

    default <P extends RelationshipPlugin<P>> P getPluginOrDefault(Class<P> pluginClass,
                                                                   P defaultPlugin) {
        return plugins().stream()
                .filter(p -> p.getPluginClass().isAssignableFrom(pluginClass))
                .findFirst()
                .map(pluginClass::cast)
                .orElse(defaultPlugin);
    }

    default List<RelationshipPlugin<?>> plugins() {
        return Collections.emptyList();
    }

}
