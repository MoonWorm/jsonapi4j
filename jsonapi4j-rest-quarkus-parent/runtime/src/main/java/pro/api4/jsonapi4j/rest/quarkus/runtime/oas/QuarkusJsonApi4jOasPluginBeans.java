package pro.api4.jsonapi4j.rest.quarkus.runtime.oas;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.customizer.AccessControlOasCustomizer;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizer;
import pro.api4.jsonapi4j.plugin.oas.customizer.SparseFieldsetsOasCustomizer;

/**
 * Optional beans that are only registered when jsonapi4j-oas-plugin is available in the app classpath.
 */
@IfBuildProperty(name = "jsonapi4j.oas.enabled", stringValue = "true")
public class QuarkusJsonApi4jOasPluginBeans {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jOasPluginBeans.class);

    @Produces
    @Singleton
    @DefaultBean
    JsonApiOasPlugin jsonApiOasPlugin(QuarkusJsonApi4jOasProperties oasProperties,
                                      Instance<OasCustomizer> customizers) {
        LOG.info("OAS Plugin Enabled. Composing {}...", JsonApiOasPlugin.class.getSimpleName());
        return new JsonApiOasPlugin(oasProperties.toOasProperties(), customizers.stream().toList());
    }

    @Produces
    @Singleton
    OasCustomizer accessControlOasCustomizer(Instance<PluginRegistry> pluginRegistry) {
        return new AccessControlOasCustomizer(pluginRegistry::get);
    }

    @Produces
    @Singleton
    OasCustomizer sparseFieldsetsOasCustomizer(Instance<PluginRegistry> pluginRegistry,
                                               Instance<DomainRegistry> domainRegistry) {
        return new SparseFieldsetsOasCustomizer(pluginRegistry::get, domainRegistry::get);
    }

}
