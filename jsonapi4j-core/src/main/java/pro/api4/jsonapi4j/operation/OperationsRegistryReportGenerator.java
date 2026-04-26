package pro.api4.jsonapi4j.operation;

import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
public class OperationsRegistryReportGenerator {

    private final OperationsRegistry operationsRegistry;

    /**
     * Prints a human-readable summary of all registered operations grouped by resource type.
     *
     * @return formatted operations registry summary
     */
    public String generateStateReport() {
        StringBuilder sb = new StringBuilder();
        sb.append("--- Operations ---\n");
        operationsRegistry.getResourceTypesWithAnyOperationConfigured()
                .stream()
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
