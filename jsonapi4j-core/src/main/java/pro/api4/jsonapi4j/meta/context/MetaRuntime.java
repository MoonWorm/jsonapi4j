package pro.api4.jsonapi4j.meta.context;

import lombok.Getter;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.PluginRegistry;

@Getter
public class MetaRuntime {

    private final MetaContext context;
    private final PluginRegistry pluginRegistry;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public MetaRuntime(MetaContext context,
                       PluginRegistry pluginRegistry,
                       DomainRegistry domainRegistry,
                       OperationsRegistry operationsRegistry) {
        this.context = Validate.notNull(context, "MetaContext must not be null");
        this.pluginRegistry = pluginRegistry == null ? PluginRegistry.empty() : pluginRegistry;
        this.domainRegistry = Validate.notNull(domainRegistry, "DomainRegistry must not be null");
        this.operationsRegistry = Validate.notNull(operationsRegistry, "OperationsRegistry must not be null");
    }

}
