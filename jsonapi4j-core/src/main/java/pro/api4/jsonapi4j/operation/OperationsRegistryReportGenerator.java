package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.domain.DomainRegistry;

public class OperationsRegistryReportGenerator {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final String headerSuffix;

    public OperationsRegistryReportGenerator(DomainRegistry domainRegistry,
                                             OperationsRegistry operationsRegistry) {
        this(domainRegistry, operationsRegistry, "");
    }

    /**
     * @param domainRegistry     supplies the built-in meta API resource types (via
     *                           {@link DomainRegistry#getMetaResourceTypes()}) to omit from this section — they are
     *                           reported separately
     * @param operationsRegistry the operations registry to summarize
     * @param headerSuffix       text appended after the {@code --- Operations ---} header (e.g. a live
     *                           introspection link); empty for no suffix
     */
    public OperationsRegistryReportGenerator(DomainRegistry domainRegistry,
                                             OperationsRegistry operationsRegistry,
                                             String headerSuffix) {
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
        this.headerSuffix = headerSuffix == null ? "" : headerSuffix;
    }

    /**
     * Prints a human-readable summary of all registered operations grouped by resource type.
     *
     * @return formatted operations registry summary
     */
    public String generateStateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Operations ---")
                .append(headerSuffix.isBlank() ? "" : "  " + headerSuffix)
                .append("\n");
        operationsRegistry.getResourceTypesWithAnyOperationConfigured()
                .stream()
                .filter(rt -> !domainRegistry.getMetaResourceTypes().contains(rt))
                .sorted()
                .forEach(rt -> {
                    String type = rt.getType();
                    sb.append("  ").append(type).append(":\n");

                    OperationType.getResourceOperationTypes().forEach(ot -> {
                        if (operationsRegistry.isResourceOperationConfigured(rt, ot)) {
                            sb.append("    ").append(ot.formatUrl(type, null)).append("\n");
                        }
                    });

                    operationsRegistry.getRelationshipNamesWithAnyOperationConfigured(rt)
                            .stream()
                            .sorted()
                            .forEach(relName -> {
                                OperationType.getAllRelationshipOperationTypes().forEach(ot -> {
                                    if (operationsRegistry.isRelationshipOperationConfigured(rt, relName, ot)) {
                                        sb.append("    ").append(ot.formatUrl(type, relName.getName())).append("\n");
                                    }
                                });
                            });
                });
        return sb.toString();
    }

}
