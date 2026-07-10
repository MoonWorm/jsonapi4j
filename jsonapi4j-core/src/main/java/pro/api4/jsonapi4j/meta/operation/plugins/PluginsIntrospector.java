package pro.api4.jsonapi4j.meta.operation.plugins;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource.PluginAttributes;

import java.util.List;
import java.util.Optional;

public interface PluginsIntrospector {

    List<PluginAttributes> plugins();

    Optional<PluginAttributes> pluginById(String id);

    List<Ref> pluginRefs();

}
