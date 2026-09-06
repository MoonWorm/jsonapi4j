package pro.api4.jsonapi4j;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.PluginProperties;
import pro.api4.jsonapi4j.config.PluginPropertiesValidationResult;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.meta.context.MetaRuntime;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.exception.OperationsMisconfigurationException;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.exception.PluginMisconfigurationException;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;

import java.text.MessageFormat;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;

public class JsonApi4jBuilder {

    private List<JsonApi4jPlugin> plugins = Collections.emptyList();
    private DomainRegistry domainRegistry = DomainRegistry.empty();
    private OperationsRegistry operationsRegistry = OperationsRegistry.empty();
    private Executor executor = ResourceProcessorContext.DEFAULT_EXECUTOR;
    private JsonApiBuildInRequestValidatorFactory validatorFactory = JsonApiBuildInRequestValidatorFactory.NO_OP;
    private MetaContext metaContext = null;

    JsonApi4jBuilder() {}

    public JsonApi4jBuilder plugins(List<JsonApi4jPlugin> plugins) {
        Validate.notNull(plugins, "Plugins must not be null");
        this.plugins = plugins;
        return this;
    }

    public JsonApi4jBuilder domainRegistry(DomainRegistry domainRegistry) {
        Validate.notNull(domainRegistry, "Domain Registry must not be null");
        this.domainRegistry = domainRegistry;
        return this;
    }

    public JsonApi4jBuilder operationsRegistry(OperationsRegistry operationsRegistry) {
        Validate.notNull(operationsRegistry, "Operations Registry must not be null");
        this.operationsRegistry = operationsRegistry;
        return this;
    }

    public JsonApi4jBuilder executor(Executor executor) {
        Validate.notNull(executor, "Executor must not be null");
        this.executor = executor;
        return this;
    }

    public JsonApi4jBuilder validatorFactory(JsonApiBuildInRequestValidatorFactory validatorFactory) {
        Validate.notNull(validatorFactory, "JsonApiBuildInRequestValidatorFactory must not be null");
        this.validatorFactory = validatorFactory;
        return this;
    }

    public JsonApi4jBuilder meta(MetaContext metaContext) {
        this.metaContext = metaContext;
        return this;
    }

    public JsonApi4j build() {
        validateIntegrity();
        validatePluginConfigs();
        if (metaContext != null) {
            domainRegistry = DomainRegistry.copy(plugins, domainRegistry).withMeta().build();
            MetaRuntime metaRuntime = new MetaRuntime(metaContext, plugins, domainRegistry, operationsRegistry);
            operationsRegistry = OperationsRegistry.copy(plugins, operationsRegistry).withMeta(metaRuntime).build();
        }
        // Materialize the validator against the final (meta-augmented) domain registry, so it never validates
        // requests against a stale, pre-meta view of the registered resources/relationships.
        JsonApiBuildInRequestValidator validator = validatorFactory.create(domainRegistry);
        return new JsonApi4j(plugins, domainRegistry, operationsRegistry, executor, validator, metaContext);
    }

    private void validateIntegrity() {
        // check if operations are pointing to the registered resources
        operationsRegistry.getAllRegisteredOperations().forEach(o -> {
            if (!domainRegistry.getResourceTypes().contains(o.getOperationMeta().getResourceType())) {
                throw new OperationsMisconfigurationException(
                        MessageFormat.format(
                                "Operation ({0}) is added for the resource that is not registered in the domain. " +
                                        "Ensure target resource is registered in the Domain Registry.",
                                o.getOperation().getClass().getSimpleName()
                        )
                );
            }
        });
    }

    /**
     * Fails the build when any enabled plugin is misconfigured, reporting every plugin's errors at once - a boot that
     * dies on the first bad key costs one restart per typo.
     */
    private void validatePluginConfigs() {
        StringBuilder errors = new StringBuilder();
        plugins.stream()
                .filter(JsonApi4jPlugin::enabled)
                .filter(plugin -> plugin.configProperties() != null)
                .forEach(plugin -> {
                    PluginProperties properties = plugin.configProperties();
                    PluginPropertiesValidationResult result = properties.validate();
                    if (result.hasErrors()) {
                        errors.append(MessageFormat.format(
                                "{0} (''{1}.{2}''):\n{3}",
                                plugin.pluginName(),
                                JsonApi4jProperties.CONFIG_PREFIX,
                                properties.section(),
                                result
                        ));
                    }
                });
        if (!errors.isEmpty()) {
            throw new PluginMisconfigurationException(
                    "Registered plugins are misconfigured. Fix the configuration and restart:\n" + errors
            );
        }
    }

}
