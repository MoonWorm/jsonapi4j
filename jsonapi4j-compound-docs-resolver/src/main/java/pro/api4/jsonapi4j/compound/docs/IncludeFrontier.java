package pro.api4.jsonapi4j.compound.docs;

import pro.api4.jsonapi4j.compound.docs.json.ResourceLinkage;
import pro.api4.jsonapi4j.processor.IdAndType;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.mapping;
import static java.util.stream.Collectors.toSet;

/**
 * The resources to visit on the current hop of compound docs resolution, each with the include paths it was reached
 * through. Immutable - {@link #next(Map)} returns the frontier of the following hop.
 */
final class IncludeFrontier {

    private final IncludeTree includeTree;
    private final Map<IdAndType, Set<String>> pathsByResource;

    private IncludeFrontier(IncludeTree includeTree, Map<IdAndType, Set<String>> pathsByResource) {
        this.includeTree = includeTree;
        this.pathsByResource = Collections.unmodifiableMap(pathsByResource);
    }

    /**
     * @param rootRelationships relationship linkage of the primary data
     * @return the first-hop frontier - resources the primary data links to through a requested relationship
     */
    static IncludeFrontier start(IncludeTree includeTree, Map<String, Set<IdAndType>> rootRelationships) {
        return new IncludeFrontier(
                includeTree,
                groupByResource(linkedResourcePaths(includeTree, Set.of(IncludeTree.ROOT), rootRelationships))
        );
    }

    /**
     * @param linkages linkage of the fetched resources; a resource of this frontier without one is not followed
     * @return the frontier of the following hop - resources linked through a relationship that continues one of the
     * paths their source was reached through
     */
    IncludeFrontier next(Map<IdAndType, ResourceLinkage> linkages) {
        return new IncludeFrontier(
                includeTree,
                groupByResource(pathsByResource.entrySet()
                        .stream()
                        .filter(e -> linkages.containsKey(e.getKey()))
                        .flatMap(e -> linkedResourcePaths(
                                includeTree,
                                e.getValue(),
                                linkages.get(e.getKey()).relationships()
                        )))
        );
    }

    boolean isEmpty() {
        return pathsByResource.isEmpty();
    }

    Set<IdAndType> resources() {
        return pathsByResource.keySet();
    }

    /**
     * @return the include paths {@code resource} was reached through
     */
    Set<String> paths(IdAndType resource) {
        return pathsByResource.getOrDefault(resource, Collections.emptySet());
    }

    /**
     * @return the include paths that continue from {@code resource} through {@code relationshipName}
     */
    Set<String> pathsThrough(IdAndType resource, String relationshipName) {
        return paths(resource)
                .stream()
                .filter(path -> includeTree.children(path).contains(relationshipName))
                .map(path -> IncludeTree.childPath(path, relationshipName))
                .collect(toSet());
    }

    /**
     * @return relationship names to request for {@code resource} so that each of its paths can continue
     */
    Set<String> requiredIncludes(IdAndType resource) {
        return paths(resource)
                .stream()
                .flatMap(path -> includeTree.children(path).stream())
                .collect(toSet());
    }

    private static Stream<ResourcePath> linkedResourcePaths(IncludeTree includeTree,
                                                            Set<String> paths,
                                                            Map<String, Set<IdAndType>> relationships) {
        return paths.stream().flatMap(path -> includeTree.children(path)
                .stream()
                .filter(relationships::containsKey)
                .flatMap(relationshipName -> relationships.get(relationshipName)
                        .stream()
                        .map(resource -> new ResourcePath(resource, IncludeTree.childPath(path, relationshipName)))));
    }

    private static Map<IdAndType, Set<String>> groupByResource(Stream<ResourcePath> resourcePaths) {
        return resourcePaths.collect(groupingBy(ResourcePath::idAndType, mapping(ResourcePath::path, toSet())));
    }

    private record ResourcePath(IdAndType idAndType, String path) {
    }

}
