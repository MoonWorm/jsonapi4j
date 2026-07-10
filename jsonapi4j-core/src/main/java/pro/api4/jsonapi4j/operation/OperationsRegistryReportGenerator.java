package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.domain.ResourceType;

import java.util.function.Predicate;

public class OperationsRegistryReportGenerator {

    private final OperationsRegistry operationsRegistry;
    private final Predicate<ResourceType> excludeResourceType;
    private final String headerSuffix;

    public OperationsRegistryReportGenerator(OperationsRegistry operationsRegistry) {
        this(operationsRegistry, resourceType -> false);
    }

    public OperationsRegistryReportGenerator(OperationsRegistry operationsRegistry,
                                             Predicate<ResourceType> excludeResourceType) {
        this(operationsRegistry, excludeResourceType, "");
    }

    /**
     * @param operationsRegistry  the operations registry to summarize
     * @param excludeResourceType predicate selecting resource types to omit from this section (e.g. the
     *                            built-in meta API types, which are reported separately)
     * @param headerSuffix        text appended after the {@code --- Operations ---} header (e.g. a live
     *                            introspection link); empty for no suffix
     */
    public OperationsRegistryReportGenerator(OperationsRegistry operationsRegistry,
                                             Predicate<ResourceType> excludeResourceType,
                                             String headerSuffix) {
        this.operationsRegistry = operationsRegistry;
        this.excludeResourceType = excludeResourceType;
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
                .filter(rt -> !excludeResourceType.test(rt))
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
