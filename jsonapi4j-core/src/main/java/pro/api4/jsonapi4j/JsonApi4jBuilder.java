package pro.api4.jsonapi4j;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.exception.OperationsMisconfigurationException;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
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
    private JsonApi4jValidator validator = JsonApi4jValidator.NO_OP;

    JsonApi4jBuilder() {}

    public JsonApi4jBuilder plugins(List<JsonApi4jPlugin> plugins) {
        this.plugins = plugins;
        return this;
    }

    public JsonApi4jBuilder domainRegistry(DomainRegistry domainRegistry) {
        this.domainRegistry = domainRegistry;
        return this;
    }

    public JsonApi4jBuilder operationsRegistry(OperationsRegistry operationsRegistry) {
        this.operationsRegistry = operationsRegistry;
        return this;
    }

    public JsonApi4jBuilder executor(Executor executor) {
        this.executor = executor;
        return this;
    }

    public JsonApi4jBuilder validator(JsonApi4jValidator validator) {
        this.validator = validator;
        return this;
    }

    public JsonApi4j build() {
        validateIntegrity();
        return new JsonApi4j(plugins, domainRegistry, operationsRegistry, executor, validator);
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

}
