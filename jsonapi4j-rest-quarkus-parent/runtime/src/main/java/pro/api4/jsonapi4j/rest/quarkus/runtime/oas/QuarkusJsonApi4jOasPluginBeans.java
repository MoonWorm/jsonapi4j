package pro.api4.jsonapi4j.rest.quarkus.runtime.oas;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;

/**
 * Optional beans that are only registered when jsonapi4j-oas-plugin is available in the app classpath.
 */
@IfBuildProperty(name = "jsonapi4j.ac.enabled", stringValue = "true")
public class QuarkusJsonApi4jOasPluginBeans {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jOasPluginBeans.class);

    @Produces
    @Singleton
    @DefaultBean
    JsonApiOasPlugin jsonApiOasPlugin(QuarkusJsonApi4jOasProperties oasProperties) {
        LOG.info("OAS Plugin Enabled. Composing {}...", JsonApiOasPlugin.class.getSimpleName());
        return new JsonApiOasPlugin(oasProperties.toOasProperties());
    }

}
