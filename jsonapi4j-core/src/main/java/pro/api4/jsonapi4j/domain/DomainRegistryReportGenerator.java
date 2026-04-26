package pro.api4.jsonapi4j.domain;

import lombok.RequiredArgsConstructor;

import java.util.Collection;

@RequiredArgsConstructor
public class DomainRegistryReportGenerator {

    private final DomainRegistry domainRegistry;

    /**
     * Prints a human-readable summary of all registered resources and their relationships.
     *
     * @return formatted domain registry summary
     */
    public String generateStateReport() {
        StringBuilder sb = new StringBuilder();
        Collection<RegisteredResource<Resource<?>>> allResources = domainRegistry.getResources();
        sb.append("--- Resources (").append(allResources.size()).append(") ---\n");
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
