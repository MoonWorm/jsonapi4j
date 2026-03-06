package pro.api4.jsonapi4j.rest.quarkus.runtime;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.principal.tier.AccessTierRegistry;

/**
 * Optional beans that are only registered when jsonapi4j-ac-plugin is available in the app classpath.
 */
public class QuarkusJsonApi4jAcPluginBeans {

    private static final String ACCESS_CONTROL_EVALUATOR_CLASS = "pro.api4.jsonapi4j.plugin.ac.AccessControlEvaluator";
    private static final String DEFAULT_ACCESS_CONTROL_EVALUATOR_CLASS = "pro.api4.jsonapi4j.plugin.ac.DefaultAccessControlEvaluator";
    private static final String ACCESS_CONTROL_PLUGIN_CLASS = "pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin";

    @Produces
    @Singleton
    @DefaultBean
    JsonApi4jPlugin jsonApiAccessControlPlugin(AccessTierRegistry accessTierRegistry) {
        return instantiateAccessControlPlugin(accessTierRegistry);
    }

    private JsonApi4jPlugin instantiateAccessControlPlugin(AccessTierRegistry accessTierRegistry) {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Class<?> evaluatorBaseClass = Class.forName(ACCESS_CONTROL_EVALUATOR_CLASS, false, classLoader);
            Class<?> defaultEvaluatorClass = Class.forName(DEFAULT_ACCESS_CONTROL_EVALUATOR_CLASS, false, classLoader);
            Class<?> pluginClass = Class.forName(ACCESS_CONTROL_PLUGIN_CLASS, false, classLoader);

            Object evaluator = defaultEvaluatorClass
                    .getConstructor(AccessTierRegistry.class)
                    .newInstance(accessTierRegistry);

            Object plugin = pluginClass
                    .getConstructor(evaluatorBaseClass)
                    .newInstance(evaluator);

            return (JsonApi4jPlugin) plugin;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Failed to instantiate JsonApiAccessControlPlugin from classpath", e);
        }
    }

}
