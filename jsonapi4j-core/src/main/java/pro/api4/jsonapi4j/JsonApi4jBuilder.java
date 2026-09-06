package pro.api4.jsonapi4j;

import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.config.DefaultJsonApi4jProperties;
import pro.api4.jsonapi4j.config.PropertiesValidationResult;
import pro.api4.jsonapi4j.config.exception.RootConfigMisconfigurationException;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.meta.context.MetaRuntime;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.exception.OperationsMisconfigurationException;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.processor.ResourceProcessorContext;

import java.text.MessageFormat;
import java.util.concurrent.Executor;

public class JsonApi4jBuilder {

    private PluginRegistry pluginRegistry = PluginRegistry.empty();
    private DomainRegistry domainRegistry = DomainRegistry.empty();
    private OperationsRegistry operationsRegistry = OperationsRegistry.empty();
    private Executor executor = ResourceProcessorContext.DEFAULT_EXECUTOR;
    private JsonApiBuildInRequestValidatorFactory validatorFactory = JsonApiBuildInRequestValidatorFactory.NO_OP;
    private MetaContext metaContext = null;
    private JsonApi4jProperties properties = new DefaultJsonApi4jProperties();

    JsonApi4jBuilder() {}

    public JsonApi4jBuilder pluginRegistry(PluginRegistry pluginRegistry) {
        Validate.notNull(pluginRegistry, "Plugin Registry must not be null");
        this.pluginRegistry = pluginRegistry;
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

    public JsonApi4jBuilder properties(JsonApi4jProperties properties) {
        Validate.notNull(properties, "JsonApi4j Properties must not be null");
        this.properties = properties;
        return this;
    }

    public JsonApi4jBuilder meta(MetaContext metaContext) {
        this.metaContext = metaContext;
        return this;
    }

    public JsonApi4j build() {
        validateIntegrity();
        validateRootConfig();
        validatePluginConfigs();
        if (metaContext != null) {
            domainRegistry = DomainRegistry.copy(pluginRegistry, domainRegistry).withMeta().build();
            MetaRuntime metaRuntime = new MetaRuntime(metaContext, pluginRegistry, domainRegistry, operationsRegistry);
            operationsRegistry = OperationsRegistry.copy(pluginRegistry, operationsRegistry).withMeta(metaRuntime).build();
        }
        // Materialize the validator against the final (meta-augmented) domain registry, so it never validates
        // requests against a stale, pre-meta view of the registered resources/relationships.
        JsonApiBuildInRequestValidator validator = validatorFactory.create(domainRegistry);
        return new JsonApi4j(pluginRegistry, domainRegistry, operationsRegistry, executor, validator, metaContext, properties);
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
     * Fails the build when the root {@code jsonapi4j} configuration is unusable. Runs before the plugin checks:
     * plugins are validated <em>against</em> the root configuration, so a broken root path would only produce
     * misleading follow-up errors.
     */
    private void validateRootConfig() {
        PropertiesValidationResult result = properties.validate();
        if (result.hasErrors()) {
            throw new RootConfigMisconfigurationException(MessageFormat.format(
                    "JsonApi4j root configuration (''{0}'') is invalid. Fix the configuration and restart:\n{1}",
                    JsonApi4jProperties.CONFIG_PREFIX,
                    result
            ));
        }
    }

    /**
     * Fails the build when any enabled plugin is misconfigured. The checks themselves live on the
     * {@link PluginRegistry}, which owns the plugins and knows which of them are active.
     */
    private void validatePluginConfigs() {
        pluginRegistry.validateConfigs(properties);
    }

}
