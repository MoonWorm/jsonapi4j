package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.plugin.sf.JsonApiSparseFieldsetsPlugin;

/**
 * Optional beans that are only registered when jsonapi4j-sf-plugin is available in the app classpath.
 */
@IfBuildProperty(name = "jsonapi4j.sf.enabled", stringValue = "true")
public class QuarkusJsonApi4jSfPluginBeans {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jSfPluginBeans.class);

    @Produces
    @Singleton
    @DefaultBean
    JsonApiSparseFieldsetsPlugin jsonApiSfPlugin() {
        LOG.info("SF Plugin Enabled. Composing {}...", JsonApiSparseFieldsetsPlugin.class.getSimpleName());
        return new JsonApiSparseFieldsetsPlugin();
    }

}
