package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.arc.DefaultBean;
import io.quarkus.arc.properties.IfBuildProperty;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;

/**
 * Optional beans that are only registered when jsonapi4j-ac-plugin is available in the app classpath.
 */
@IfBuildProperty(name = "jsonapi4j.ac.enabled", stringValue = "true")
public class QuarkusJsonApi4jAcPluginBeans {

    private static final Logger LOG = LoggerFactory.getLogger(QuarkusJsonApi4jAcPluginBeans.class);

    @Produces
    @Singleton
    @DefaultBean
    JsonApiAccessControlPlugin jsonApiAccessControlPlugin(AccessControlEvaluator accessControlEvaluator,
                                                          QuarkusJsonApi4jAcProperties acProperties) {
        LOG.info("AC Plugin Enabled. Composing {}...", JsonApiAccessControlPlugin.class.getSimpleName());
        return new JsonApiAccessControlPlugin(accessControlEvaluator, acProperties.toJsonapi4jAcProperties());
    }

    @Produces
    @Singleton
    @DefaultBean
    AccessControlEvaluator accessControlEvaluator(AccessTierRegistry accessTierRegistry) {
        LOG.info(
                "AC Plugin Enabled. Composing {} as {}",
                AccessControlEvaluator.class.getSimpleName(),
                DefaultAccessControlEvaluator.class.getSimpleName()
        );
        return new DefaultAccessControlEvaluator(accessTierRegistry);
    }

}
