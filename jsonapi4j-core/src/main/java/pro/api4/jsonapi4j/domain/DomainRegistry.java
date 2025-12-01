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
    private final Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> allRelationships;
    private final Map<ResourceType, Map<RelationshipName, ToOneRelationship<?, ?>>> toOneRelationships;
    private final Map<ResourceType, Map<RelationshipName, ToManyRelationship<?, ?>>> toManyRelationships;

    private DomainRegistry(
            Map<ResourceType, Resource<?>> resources,
            Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> allRelationships,
            Map<ResourceType, Map<RelationshipName, ToOneRelationship<?, ?>>> toOneRelationships,
            Map<ResourceType, Map<RelationshipName, ToManyRelationship<?, ?>>> toManyRelationships
    ) {
        this.resources = MapUtils.emptyIfNull(resources);
        this.allRelationships = MapUtils.emptyIfNull(allRelationships);
        this.toOneRelationships = MapUtils.emptyIfNull(toOneRelationships);
        this.toManyRelationships = MapUtils.emptyIfNull(toManyRelationships);
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
        return this.resources.get(resourceType);
    }

    public Collection<Resource<?>> getResources() {
        return Collections.unmodifiableCollection(this.resources.values());
    }

    public Set<ResourceType> getResourceTypes() {
        return Collections.unmodifiableSet(this.resources.keySet());
    }

    public List<ToManyRelationship<?, ?>> getToManyRelationships(ResourceType resourceType) {
        return MapUtils.emptyIfNull(this.toManyRelationships.get(resourceType))
                .values()
                .stream()
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
        return MapUtils.emptyIfNull(this.toOneRelationships.get(resourceType))
                .values()
                .stream()
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
        Map<RelationshipName, ToManyRelationship<?, ?>> resourceRelationships = this.toManyRelationships.get(resourceType);
        if (MapUtils.isEmpty(resourceRelationships)) {
            throw new DomainMisconfigurationException("No To Many relationships found for the Resource ("
                    + resourceType.getType() + ").");
        }
        ToManyRelationship<?, ?> result = resourceRelationships.get(relationshipName);
        if (result == null) {
            throw new DomainMisconfigurationException("Implementation of the '"
                    + relationshipName.getName() + "' To Many Relationship is not found for the Resource ("
                    + resourceType.getType() + "). Please implement the relationship.");
        }
        return result;
    }

    public ToOneRelationship<?, ?> getToOneRelationshipStrict(ResourceType resourceType,
                                                              RelationshipName relationshipName) {
        Map<RelationshipName, ToOneRelationship<?, ?>> resourceRelationships = this.toOneRelationships.get(resourceType);
        if (MapUtils.isEmpty(resourceRelationships)) {
            throw new DomainMisconfigurationException("No To One relationships found for the Resource ("
                    + resourceType.getType() + ").");
        }
        ToOneRelationship<?, ?> result = resourceRelationships.get(relationshipName);
        if (result == null) {
            throw new DomainMisconfigurationException("Implementation of the '"
                    + relationshipName.getName() + "' To One Relationship is not found for the Resource ("
                    + resourceType.getType() + "). Please implement the relationship.");
        }
        return result;
    }

    public Set<RelationshipName> getAvailableRelationshipNames(ResourceType resourceType) {
        return MapUtils.emptyIfNull(allRelationships.get(resourceType))
                .values()
                .stream()
                .map(Relationship::relationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Slf4j
    public static class DomainRegistryBuilder {

        private final Map<ResourceType, Resource<?>> resources;
        private final Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> allRelationships;
        private final Map<ResourceType, Map<RelationshipName, ToOneRelationship<?, ?>>> toOneRelationships;
        private final Map<ResourceType, Map<RelationshipName, ToManyRelationship<?, ?>>> toManyRelationships;

        private DomainRegistryBuilder() {
            this.resources = new HashMap<>();
            this.allRelationships = new HashMap<>();
            this.toOneRelationships = new HashMap<>();
            this.toManyRelationships = new HashMap<>();
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
            if (this.allRelationships.containsKey(relationship.resourceType())
                    && this.allRelationships.get(relationship.resourceType()).containsKey(relationship.relationshipName())) {
                throw new DomainMisconfigurationException("Multiple relationship declarations found for : "
                        + relationship.resourceType() + " resource type, for relationship: " + relationship.relationshipName());
            }
            this.allRelationships.computeIfAbsent(
                    relationship.resourceType(),
                    k -> new HashMap<>()
            ).put(
                    relationship.relationshipName(),
                    relationship
            );

            // pre-filter relationship by type
            if (relationship instanceof ToOneRelationship<?, ?> rel) {
                this.toOneRelationships.computeIfAbsent(
                        relationship.resourceType(),
                        k -> new HashMap<>()
                ).put(relationship.relationshipName(), rel);
            }
            if (relationship instanceof ToManyRelationship<?, ?> rel) {
                this.toManyRelationships.computeIfAbsent(
                        relationship.resourceType(),
                        k -> new HashMap<>()
                ).put(relationship.relationshipName(), rel);
            }

            log.info("{} relationship with '{}' name has been registered for '{}' type.", relationship.getClass().getSimpleName(), relationship.relationshipName(), relationship.resourceType().getType());
            return this;
        }

        public DomainRegistry build() {
            return new DomainRegistry(
                    Collections.unmodifiableMap(this.resources),
                    Collections.unmodifiableMap(this.allRelationships),
                    Collections.unmodifiableMap(this.toOneRelationships),
                    Collections.unmodifiableMap(this.toManyRelationships)
            );
        }
    }

}
