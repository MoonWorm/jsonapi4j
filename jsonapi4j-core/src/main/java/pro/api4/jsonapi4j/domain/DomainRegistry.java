package pro.api4.jsonapi4j.domain;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class DomainRegistry {

    private final Map<ResourceType, Resource<?>> resources;
    private final Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> relationships;

    private DomainRegistry(
            Map<ResourceType, Resource<?>> resources,
            Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> relationships
    ) {
        this.resources = resources;
        this.relationships = relationships;
    }

    public static DomainRegistryBuilder builder() {
        return new DomainRegistryBuilder();
    }

    public static DomainRegistry empty() {
        return DomainRegistry.builder()
                .resources(Collections.emptySet())
                .relationships(Collections.emptySet())
                .build();
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
        return MapUtils.emptyIfNull(relationships.get(resourceType))
                .values()
                .stream()
                .filter(rel -> rel instanceof ToManyRelationship<?, ?>)
                .map(rel -> (ToManyRelationship<?, ?>) rel)
                .collect(Collectors.toUnmodifiableList());
    }

    public Set<RelationshipName> getToManyRelationshipNames(ResourceType resourceType) {
        return getToManyRelationships(resourceType)
                .stream()
                .map(Relationship::relationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<ToOneRelationship<?, ?>> getToOneRelationships(ResourceType resourceType) {
        return MapUtils.emptyIfNull(relationships.get(resourceType))
                .values()
                .stream()
                .filter(rel -> rel instanceof ToOneRelationship<?, ?>)
                .map(rel -> (ToOneRelationship<?, ?>) rel)
                .collect(Collectors.toUnmodifiableList());
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

    @Slf4j
    public static class DomainRegistryBuilder {

        private final Map<ResourceType, Resource<?>> resources;
        private final Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> relationships;

        private DomainRegistryBuilder() {
            this.resources = new HashMap<>();
            this.relationships = new HashMap<>();
        }

        public DomainRegistryBuilder resources(Set<Resource<?>> resources) {
            for (Resource<?> resource : resources) {
                this.resource(resource);
            }
            return this;
        }

        public DomainRegistryBuilder resource(Resource<?> resource) {
            if (this.resources.containsKey(resource.resourceType())) {
                throw new DomainMisconfigurationException("Multiple resource declarations found for : "
                        + resource.resourceType() + " resource type");
            }
            this.resources.put(resource.resourceType(), resource);
            log.info("{} resource of '{}' type has been registered.", resource.getClass().getSimpleName(), resource.resourceType().getType());
            return this;
        }

        public DomainRegistryBuilder relationships(Set<Relationship<?, ?>> relationships) {
            for (Relationship<?, ?> relationship : relationships) {
                this.relationship(relationship);
            }
            return this;
        }

        public DomainRegistryBuilder relationship(Relationship<?, ?> relationship) {
            if (this.relationships.containsKey(relationship.parentResourceType())
                    && this.relationships.get(relationship.parentResourceType()).containsKey(relationship.relationshipName())) {
                throw new DomainMisconfigurationException("Multiple relationship declarations found for : "
                        + relationship.parentResourceType() + " resource type, for relationship: " + relationship.relationshipName());
            }
            this.relationships.computeIfAbsent(
                    relationship.parentResourceType(),
                    k -> new HashMap<>()
            ).put(
                    relationship.relationshipName(),
                    relationship
            );
            log.info("{} relationship with '{}' name has been registered for '{}' type.", relationship.getClass().getSimpleName(), relationship.relationshipName(), relationship.parentResourceType().getType());
            return this;
        }

        public DomainRegistry build() {
            return new DomainRegistry(
                    Collections.unmodifiableMap(this.resources),
                    Collections.unmodifiableMap(this.relationships)
            );
        }
    }

}
