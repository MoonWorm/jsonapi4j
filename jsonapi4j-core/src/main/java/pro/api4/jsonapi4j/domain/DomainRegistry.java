package pro.api4.jsonapi4j.domain;


import pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException;
import org.apache.commons.collections4.MapUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DomainRegistry {

    public static final DomainRegistry EMPTY = new DomainRegistry(
            Collections.emptySet(),
            Collections.emptySet()
    );

    private final Map<ResourceType, Resource<?>> resources;
    private final Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> relationships;

    public DomainRegistry(
            Set<Resource<?>> resources,
            Set<Relationship<?, ?>> relationships
    ) {
        this.resources = Collections.unmodifiableMap(
                resources.stream().collect(Collectors.groupingBy(Resource::resourceType))
                        .entrySet()
                        .stream()
                        .collect(Collectors.toMap(
                                Map.Entry::getKey,
                                e -> {
                                    if (e.getValue().size() > 1) {
                                        throw new DomainMisconfigurationException("Multiple resource declarations found for : " + e.getKey().getType() + " resource type");
                                    }
                                    return e.getValue().getFirst();
                                }
                        ))
        );

        this.relationships = Collections.unmodifiableMap(
                relationships.stream()
                        .collect(Collectors.groupingBy(Relationship::parentResourceType))
                        .entrySet()
                        .stream()
                        .collect(
                                Collectors.toMap(
                                        Map.Entry::getKey,
                                        resourceEntry -> resourceEntry.getValue().stream().collect(Collectors.groupingBy(Relationship::relationshipName))
                                                .entrySet()
                                                .stream()
                                                .collect(Collectors.toMap(
                                                        Map.Entry::getKey,
                                                        relationshipEntry -> {
                                                            if (relationshipEntry.getValue().size() > 1) {
                                                                throw new DomainMisconfigurationException("Multiple relationship declarations found for : " + resourceEntry.getKey().getType() + " resource type, for relationship: " + relationshipEntry.getKey().getName());
                                                            }
                                                            return relationshipEntry.getValue().getFirst();
                                                        }
                                                ))
                                )
                        )
        );
    }

    public Resource<?> getResource(ResourceType resourceType) {
        return resources.get(resourceType);
    }

    public Collection<Resource<?>> getResources() {
        return Collections.unmodifiableCollection(resources.values());
    }

    public Set<ResourceType> getResourceTypes() {
        return Collections.unmodifiableSet(resources.keySet());
    }

    public List<ToManyRelationship<?, ?>> getToManyRelationships(ResourceType resourceType) {
        return Collections.unmodifiableList(
                MapUtils.emptyIfNull(relationships.get(resourceType))
                        .values()
                        .stream()
                        .filter(rel -> rel instanceof ToManyRelationship<?, ?>)
                        .map(rel -> (ToManyRelationship<?, ?>) rel)
                        .toList()
        );
    }

    public Set<RelationshipName> getToManyRelationshipNames(ResourceType resourceType) {
        return getToManyRelationships(resourceType)
                .stream()
                .map(Relationship::relationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<ToOneRelationship<?, ?>> getToOneRelationships(ResourceType resourceType) {
        return Collections.unmodifiableList(
                MapUtils.emptyIfNull(relationships.get(resourceType))
                        .values()
                        .stream()
                        .filter(rel -> rel instanceof ToOneRelationship<?, ?>)
                        .map(rel -> (ToOneRelationship<?, ?>) rel)
                        .toList()
        );
    }

    public Set<RelationshipName> getToOneRelationshipNames(ResourceType resourceType) {
        return getToOneRelationships(resourceType)
                .stream()
                .map(Relationship::relationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public ToManyRelationship<?, ?> getToManyRelationshipStrict(ResourceType resourceType,
                                                                RelationshipName relationshipName) {
        Map<RelationshipName, Relationship<?, ?>> resourceRelationships = relationships.get(resourceType);
        if (MapUtils.isEmpty(resourceRelationships)) {
            throw new DomainMisconfigurationException("No relationships found for the Resource ("
                    + resourceType.getType() + ").");
        }
        Relationship<?, ?> result = resourceRelationships.get(relationshipName);
        if (result == null) {
            throw new DomainMisconfigurationException("Implementation of the '"
                    + relationshipName.getName() + "' To Many Relationship is not found for the Resource ("
                    + resourceType.getType() + "). Please implement the relationship.");
        }
        if (result instanceof ToManyRelationship<?, ?> rel) {
            return rel;
        } else {
            throw new DomainMisconfigurationException("Found implementation of the '"
                    + relationshipName.getName() + "' relationship for the Resource ("
                    + resourceType.getType() + ") but it's not a To Many Relationship. Please implement the " +
                    "relationship or approach the relationship as a To One Relationship.");
        }
    }

    public ToOneRelationship<?, ?> getToOneRelationshipStrict(ResourceType resourceType,
                                                              RelationshipName relationshipName) {
        Map<RelationshipName, Relationship<?, ?>> resourceRelationships = relationships.get(resourceType);
        if (MapUtils.isEmpty(resourceRelationships)) {
            throw new DomainMisconfigurationException("No relationships found for the Resource ("
                    + resourceType.getType() + ").");
        }
        Relationship<?, ?> result = resourceRelationships.get(relationshipName);
        if (result == null) {
            throw new DomainMisconfigurationException("Implementation of the '"
                    + relationshipName.getName() + "' To One Relationship is not found for the Resource ("
                    + resourceType.getType() + "). Please implement the relationship.");
        }
        if (result instanceof ToOneRelationship<?, ?> rel) {
            return rel;
        } else {
            throw new DomainMisconfigurationException("Found implementation of the '"
                    + relationshipName.getName() + "' relationship for the Resource ("
                    + resourceType.getType() + ") but it's not a To One Relationship. Please implement the " +
                    "relationship or approach the relationship as a To Many Relationship.");
        }
    }

    public Set<RelationshipName> getAvailableRelationshipNames(ResourceType resourceType) {
        return MapUtils.emptyIfNull(relationships.get(resourceType))
                .values()
                .stream()
                .map(Relationship::relationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

}
