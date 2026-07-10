package pro.api4.jsonapi4j.domain;

import java.util.Collection;
import java.util.Set;

public class DomainRegistryReportGenerator {

    private final DomainRegistry domainRegistry;
    private final String headerSuffix;

    public DomainRegistryReportGenerator(DomainRegistry domainRegistry) {
        this(domainRegistry, "");
    }

    /**
     * @param domainRegistry the domain registry to summarize
     * @param headerSuffix   text appended after the {@code --- Domain (N) ---} header (e.g. live
     *                       introspection links); empty for no suffix
     */
    public DomainRegistryReportGenerator(DomainRegistry domainRegistry,
                                         String headerSuffix) {
        this.domainRegistry = domainRegistry;
        this.headerSuffix = headerSuffix == null ? "" : headerSuffix;
    }

    /**
     * Prints a human-readable summary of all registered resources and their relationships.
     *
     * @return formatted domain registry summary
     */
    public String generateStateReport() {
        StringBuilder sb = new StringBuilder();
        Set<ResourceType> metaResourceTypes = domainRegistry.getMetaResourceTypes();
        Collection<RegisteredResource<Resource<?>>> allResources = domainRegistry.getResources().stream()
                .filter(rr -> !metaResourceTypes.contains(rr.getResourceType()))
                .toList();
        sb.append("--- Domain (").append(allResources.size()).append(") ---")
                .append(headerSuffix.isBlank() ? "" : "  " + headerSuffix)
                .append("\n");
        allResources.stream()
                .sorted()
                .forEach(rr -> {
                    ResourceType rt = rr.getResourceType();
                    sb.append("  ").append(rt.getType())
                            .append(" (").append(rr.getRegisteredAs().getSimpleName()).append(")\n");

                    domainRegistry.getToOneRelationships(rt).forEach(rel ->
                            sb.append("    -> ").append(rel.getRelationshipName().getName())
                                    .append(" [TO_ONE]\n"));

                    domainRegistry.getToManyRelationships(rt).forEach(rel ->
                            sb.append("    -> ").append(rel.getRelationshipName().getName())
                                    .append(" [TO_MANY]\n"));
                });
        return sb.toString();
    }

}
