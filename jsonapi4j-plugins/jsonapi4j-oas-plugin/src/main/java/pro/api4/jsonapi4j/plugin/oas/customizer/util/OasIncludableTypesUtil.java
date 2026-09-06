package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RegisteredRelationship;
import pro.api4.jsonapi4j.domain.RegisteredResource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasRelationshipInfoModel;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;

/**
 * Resolves the resource types a document may pull in alongside its primary data — the targets a relationship declares
 * through {@link pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo#relationshipTypes()}.
 * <p>
 * One level only. A client may nest includes ({@code include=citizenships.currencies}) and reach further, but the
 * document describes the immediate contract of the operation it belongs to and leaves the rest of the graph to be
 * discovered from the related resource's own operations. Both the {@code included} schema and the {@code fields[…]}
 * parameters read this same set, so the two cannot describe different depths.
 */
public final class OasIncludableTypesUtil {

    private OasIncludableTypesUtil() {
    }

    /**
     * Returns the types reachable in one hop from the given resource, in a stable order and without duplicates —
     * a relationship graph may well point back at where it started.
     */
    public static Set<ResourceType> includableResourceTypes(DomainRegistry domainRegistry,
                                                            ResourceType resourceType) {
        return Stream.concat(
                        domainRegistry.getToOneRelationships(resourceType).stream(),
                        domainRegistry.getToManyRelationships(resourceType).stream()
                )
                .flatMap(relationship -> declaredTargetTypes(domainRegistry, relationship))
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    /**
     * Returns the types whose fields a request against this resource may select: the resource itself plus everything
     * it can include.
     */
    public static Set<ResourceType> sparseFieldsetsResourceTypes(DomainRegistry domainRegistry,
                                                                ResourceType resourceType) {
        Set<ResourceType> types = new LinkedHashSet<>();
        types.add(resourceType);
        types.addAll(includableResourceTypes(domainRegistry, resourceType));
        return types;
    }

    private static Stream<ResourceType> declaredTargetTypes(DomainRegistry domainRegistry,
                                                            RegisteredRelationship<?> relationship) {
        Object pluginInfo = emptyIfNull(relationship.getPluginInfo()).get(JsonApiOasPlugin.NAME);
        if (!(pluginInfo instanceof OasRelationshipInfoModel oasRelationshipInfo)) {
            return Stream.empty();
        }
        List<Class<? extends pro.api4.jsonapi4j.domain.Resource<?>>> relationshipTypes
                = oasRelationshipInfo.getRelationshipTypes();
        return relationshipTypes.stream()
                .map(domainRegistry::getResource)
                .map(RegisteredResource::getResourceType);
    }

}
