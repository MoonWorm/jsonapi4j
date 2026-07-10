package pro.api4.jsonapi4j;

import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.config.RawConfigAccessor;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.meta.domain.config.ConfigResource;
import pro.api4.jsonapi4j.meta.domain.operations.OperationsResource;
import pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource;
import pro.api4.jsonapi4j.meta.domain.relationships.RelationshipsResource;
import pro.api4.jsonapi4j.meta.domain.resources.ResourcesResource;
import pro.api4.jsonapi4j.meta.domain.state.StateResource;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistryReportGenerator;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.List;

import static pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain.isMetaResource;

public class JsonApi4jReportGenerator {

    private static final String BANNER = """
            
                   ██                               ███              ██     ████  ███    \s
                   ██                              █████                   █████         \s
                   ██  ██████▒ ███████  ███████   ███ ██   ████████  ██   ██ ███   ██    \s
                   ██  █████   ██   ███ ███  ███  ███ ███  ███   ███ ██  ██  ███   ██    \s
              ██   ██     ████ ██   ███ ██▒  ███ ████████▓ ███   ██▓ ██ █████████  ██    \s
              ███████  ███████ ███████  ██▒  ███ ██    ███ ████████  ██      ███   ██    \s
                                                           ███                    ███    \s
                                                            ██                   ███     \s
            
            """;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final List<JsonApi4jPlugin> plugins;
    private final MetaHelper metaHelper;

    /**
     * @param jsonApi4j the assembled framework instance. When its {@link JsonApi4j#getMetaContext() meta context} is
     *                  present (meta enabled), the report is enriched with live introspection URLs per section.
     */
    public JsonApi4jReportGenerator(JsonApi4j jsonApi4j) {
        this.domainRegistry = jsonApi4j.getDomainRegistry();
        this.operationsRegistry = jsonApi4j.getOperationsRegistry();
        this.plugins = jsonApi4j.getPlugins();
        this.metaHelper = new MetaHelper(jsonApi4j.getMetaContext());
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
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(BANNER);
        sb.append(metaHelper.generateFullMetaInfoHeader());
        sb.append(generatePluginsReport());
        sb.append("\n").append(new DomainRegistryReportGenerator(
                domainRegistry, metaHelper.domainHeaderLinks()).generateStateReport());
        sb.append("\n").append(new OperationsRegistryReportGenerator(
                operationsRegistry, this::isMetaType, metaHelper.operationsHeaderLink()).generateStateReport());
        sb.append(generateConfigSectionReport());
        sb.append(generateCrossDomainAndOperationsReport());
        return sb.toString();
    }

    /**
     * Whether the given resource type belongs to the built-in meta API. Such types are surfaced only via the live
     * introspection links, never mixed into the host's domain sections. Always {@code false} when the meta API is
     * disabled/absent, so legacy output is unchanged.
     */
    private boolean isMetaType(ResourceType resourceType) {
        return metaHelper.metaEnabled() && isMetaResource(resourceType);
    }

    private String generateConfigSectionReport() {
        return metaHelper.metaEnabled()
                ? "\n--- Config ---  (" + metaHelper.configHeaderLink() + ")\n"
                : "";
    }

    private String generatePluginsReport() {
        StringBuilder sb = new StringBuilder();
        List<String> enabledPlugins = plugins.stream()
                .filter(JsonApi4jPlugin::enabled)
                .map(JsonApi4jPlugin::pluginName)
                .toList();
        sb.append("\n--- Plugins (").append(enabledPlugins.size()).append(") ---")
                .append(metaHelper.metaEnabled() ? "  (" + metaHelper.metaResourceUrl(PluginsResource.PLUGINS) + ")" : "")
                .append("\n");
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
                .filter(rt -> !isMetaType(rt))
                .filter(rt -> !operationsRegistry.isAnyResourceOperationConfigured(rt))
                .toList();
        List<RelationshipName> toOneRelationshipsWithoutOperations = domainRegistry.getResourceTypes()
                .stream()
                .flatMap(rt -> domainRegistry.getToOneRelationships(rt).stream())
                .filter(rel -> !isMetaType(rel.getParentResourceType()))
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
                .filter(rel -> !isMetaType(rel.getParentResourceType()))
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

    @RequiredArgsConstructor
    private static class MetaHelper {

        /**
         * The {@code jsonapi4j.cd} config subtree and its {@code enabled} flag. These belong to the Compound Docs plugin
         * (which core cannot depend on), so they are referenced here as plain config keys read from the effective snapshot.
         */
        private static final String CD_SECTION = "cd";
        private static final String CD_ENABLED_KEY = "enabled";
        private final MetaContext metaContext;

        boolean metaEnabled() {
            return metaContext != null;
        }

        String generateFullMetaInfoHeader() {
            if (metaContext == null) {
                return "";
            }
            if (!compoundDocsEnabled()) {
                String stateUrl = metaUrl("/" + StateResource.STATE + "/" + MetaDomain.SINGLETON_ID);
                return "\nLive introspection: " + stateUrl
                        + "\n  Note: one-request `?include=...` compound documents require the Compound Docs plugin"
                        + " (jsonapi4j.cd.enabled=true); query the per-section links below directly instead.\n";
            }
            String everything = String.join(",",
                    PluginsResource.PLUGINS,
                    ResourcesResource.RESOURCES,
                    RelationshipsResource.RELATIONSHIPS,
                    OperationsResource.OPERATIONS,
                    ConfigResource.CONFIG);
            String stateUrl = metaUrl("/" + StateResource.STATE + "/" + MetaDomain.SINGLETON_ID
                    + "?include=" + everything);
            return "\nLive introspection: " + stateUrl + "\n";
        }

        String metaResourceUrl(String type) {
            return metaUrl("/" + type);
        }

        String domainHeaderLinks() {
            return metaContext != null
                    ? "(" + metaResourceUrl(ResourcesResource.RESOURCES) + ")  (" + metaResourceUrl(RelationshipsResource.RELATIONSHIPS) + ")"
                    : "";
        }

        String operationsHeaderLink() {
            return metaContext != null ? "(" + metaResourceUrl(OperationsResource.OPERATIONS) + ")" : "";
        }

        String configHeaderLink() {
            return metaUrl("/" + ConfigResource.CONFIG + "/" + MetaDomain.SINGLETON_ID);
        }

        private String metaUrl(String pathAndQuery) {
            return metaContext.getRootPath() + pathAndQuery;
        }

        private boolean compoundDocsEnabled() {
            return new RawConfigAccessor(metaContext.getConfig())
                    .section(CD_SECTION)
                    .flatMap(cd -> cd.boolValue(CD_ENABLED_KEY))
                    .orElse(false);
        }

    }

}
