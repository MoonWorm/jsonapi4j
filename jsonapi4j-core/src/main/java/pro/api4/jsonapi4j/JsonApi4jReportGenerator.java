package pro.api4.jsonapi4j;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.DomainRegistryReportGenerator;
import pro.api4.jsonapi4j.domain.RegisteredRelationship;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistryReportGenerator;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.text.MessageFormat;
import java.util.List;

public class JsonApi4jReportGenerator {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final List<JsonApi4jPlugin> plugins;

    public JsonApi4jReportGenerator(JsonApi4j jsonApi4j) {
        this.domainRegistry = jsonApi4j.getDomainRegistry();
        this.operationsRegistry = jsonApi4j.getOperationsRegistry();
        this.plugins = jsonApi4j.getPlugins();
    }

    /**
     * Prints a human-readable summary of the current JsonApi4j state including:
     * <ul>
     *     <li>All registered resources and their relationships</li>
     *     <li>All registered operations grouped by resource type</li>
     *     <li>Active plugins</li>
     * </ul>
     *
     * @return formatted state summary
     */
    public String generateStateReport() {
        return MessageFormat.format(
                "=== JsonApi4j State ===\n\n{0}\n{1}\n{2}\n{3}",
                generatePluginsReport(),
                new DomainRegistryReportGenerator(domainRegistry).generateStateReport(),
                new OperationsRegistryReportGenerator(operationsRegistry).generateStateReport(),
                generateCrossDomainAndOperationsReport()
        );
    }

    private String generatePluginsReport() {
        StringBuilder sb = new StringBuilder();
        List<String> enabledPlugins = plugins.stream()
                .filter(JsonApi4jPlugin::enabled)
                .map(JsonApi4jPlugin::pluginName)
                .toList();
        sb.append("\n--- Plugins (").append(enabledPlugins.size()).append(") ---\n");
        if (enabledPlugins.isEmpty()) {
            sb.append("  (none)\n");
        } else {
            enabledPlugins.forEach(name -> sb.append("  - ").append(name).append("\n"));
        }
        return sb.toString();
    }

    private String generateCrossDomainAndOperationsReport() {
        StringBuilder sb = new StringBuilder();
        List<ResourceType> resourcesWithoutOperations = domainRegistry.getResourceTypes()
                .stream()
                .filter(rt -> !operationsRegistry.isAnyResourceOperationConfigured(rt))
                .toList();
        List<RelationshipName> toOneRelationshipsWithoutOperations = domainRegistry.getResourceTypes()
                .stream()
                .flatMap(rt -> domainRegistry.getToOneRelationships(rt).stream())
                .filter(rel -> !operationsRegistry.isAnyToOneRelationshipOperationConfigured(
                        rel.getParentResourceType(),
                        rel.getRelationshipName())
                )
                .map(RegisteredRelationship::getRelationshipName)
                .sorted()
                .toList();
        List<RelationshipName> toManyRelationshipsWithoutOperations = domainRegistry.getResourceTypes()
                .stream()
                .flatMap(rt -> domainRegistry.getToManyRelationships(rt).stream())
                .filter(rel -> !operationsRegistry.isAnyToManyRelationshipOperationConfigured(
                        rel.getParentResourceType(),
                        rel.getRelationshipName())
                )
                .map(RegisteredRelationship::getRelationshipName)
                .sorted()
                .toList();
        if (!resourcesWithoutOperations.isEmpty()
                || !toOneRelationshipsWithoutOperations.isEmpty()
                || !toManyRelationshipsWithoutOperations.isEmpty()) {
            sb.append("\n--- Warnings")
                    .append(resourcesWithoutOperations.size() + toOneRelationshipsWithoutOperations.size() + toManyRelationshipsWithoutOperations.size())
                    .append(") ---\n");
            if (!resourcesWithoutOperations.isEmpty()) {
                sb.append("  Resources without any operations implemented:\n");
                resourcesWithoutOperations.forEach(rt -> sb.append("    - ").append(rt.getType()).append("\n"));
            }
            if (!toOneRelationshipsWithoutOperations.isEmpty()) {
                sb.append("  To-One Relationships without any operations implemented:\n");
                toOneRelationshipsWithoutOperations.forEach(rn -> sb.append("    - ").append(rn.getName()).append("\n"));
            }
            if (!toManyRelationshipsWithoutOperations.isEmpty()) {
                sb.append("  To-Many Relationships without any operations implemented:\n");
                toManyRelationshipsWithoutOperations.forEach(rn -> sb.append("    - ").append(rn.getName()).append("\n"));
            }
        }
        return sb.toString();
    }

}
