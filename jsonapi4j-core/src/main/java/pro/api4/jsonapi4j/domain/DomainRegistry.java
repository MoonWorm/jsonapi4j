package pro.api4.jsonapi4j.domain;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class DomainRegistry {

    private final Map<ResourceType, RegisteredResource<Resource<?>>> resources;
    private final Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> allRelationships;
    private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToOneRelationship<?, ?>>>> toOneRelationships;
    private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToManyRelationship<?, ?>>>> toManyRelationships;

    private DomainRegistry(
            Map<ResourceType, RegisteredResource<Resource<?>>> resources,
            Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> allRelationships,
            Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToOneRelationship<?, ?>>>> toOneRelationships,
            Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToManyRelationship<?, ?>>>> toManyRelationships
    ) {
        this.resources = MapUtils.emptyIfNull(resources);
        this.allRelationships = MapUtils.emptyIfNull(allRelationships);
        this.toOneRelationships = MapUtils.emptyIfNull(toOneRelationships);
        this.toManyRelationships = MapUtils.emptyIfNull(toManyRelationships);
    }

    public static DomainRegistryBuilder builder(List<JsonApi4jPlugin> plugins) {
        return new DomainRegistryBuilder(plugins);
    }

    public static DomainRegistry empty() {
        return DomainRegistry.builder(Collections.emptyList())
                .resources(Collections.emptySet())
                .relationships(Collections.emptySet())
                .build();
    }

    public Resource<?> getResource(ResourceType resourceType) {
        RegisteredResource<Resource<?>> registeredResource = this.resources.get(resourceType);
        if (registeredResource != null) {
            return registeredResource.getResource();
        }
        return null;
    }

    public RegisteredResource<Resource<?>> getRegisteredResource(ResourceType resourceType) {
        return this.resources.get(resourceType);
    }

    public Collection<Resource<?>> getResources() {
        return Collections.unmodifiableCollection(this.resources.values().stream().map(RegisteredResource::getResource).toList());
    }

    public Collection<RegisteredResource<Resource<?>>> getRegisteredResources() {
        return Collections.unmodifiableCollection(this.resources.values());
    }

    public Set<ResourceType> getResourceTypes() {
        return Collections.unmodifiableSet(this.resources.keySet());
    }

    public List<? extends ToManyRelationship<?, ?>> getToManyRelationships(ResourceType resourceType) {
        return getRegisteredToManyRelationships(resourceType).stream().map(RegisteredRelationship::getRelationship).toList();
    }

    public List<RegisteredRelationship<ToManyRelationship<?, ?>>> getRegisteredToManyRelationships(ResourceType resourceType) {
        return MapUtils.emptyIfNull(this.toManyRelationships.get(resourceType))
                .values()
                .stream()
                .toList();
    }

    public Set<RelationshipName> getToManyRelationshipNames(ResourceType resourceType) {
        return getToManyRelationships(resourceType)
                .stream()
                .map(Relationship::relationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<? extends ToOneRelationship<?, ?>> getToOneRelationships(ResourceType resourceType) {
        return getRegisteredToOneRelationships(resourceType)
                .stream()
                .map(RegisteredRelationship::getRelationship)
                .toList();
    }

    public List<RegisteredRelationship<ToOneRelationship<?, ?>>> getRegisteredToOneRelationships(
            ResourceType resourceType
    ) {
        return MapUtils.emptyIfNull(this.toOneRelationships.get(resourceType))
                .values()
                .stream()
                .toList();
    }

    public Set<RelationshipName> getToOneRelationshipNames(ResourceType resourceType) {
        return getToOneRelationships(resourceType)
                .stream()
                .map(Relationship::relationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public ToManyRelationship<?, ?> getToManyRelationshipStrict(ResourceType resourceType,
                                                                RelationshipName relationshipName) {
        return getRegisteredToManyRelationshipStrict(resourceType, relationshipName).getRelationship();

    }

    public RegisteredRelationship<ToManyRelationship<?, ?>> getRegisteredToManyRelationshipStrict(
            ResourceType resourceType,
            RelationshipName relationshipName
    ) {
        Map<RelationshipName, RegisteredRelationship<ToManyRelationship<?, ?>>> resourceRelationships = this.toManyRelationships.get(resourceType);
        if (MapUtils.isEmpty(resourceRelationships)) {
            throw new DomainMisconfigurationException("No To Many relationships found for the Resource ("
                    + resourceType.getType() + ").");
        }
        RegisteredRelationship<ToManyRelationship<?, ?>> result = resourceRelationships.get(relationshipName);
        if (result == null) {
            throw new DomainMisconfigurationException("Implementation of the '"
                    + relationshipName.getName() + "' To Many Relationship is not found for the Resource ("
                    + resourceType.getType() + "). Please implement the relationship.");
        }
        return result;
    }

    public ToOneRelationship<?, ?> getToOneRelationshipStrict(ResourceType resourceType,
                                                              RelationshipName relationshipName) {
        return getRegisteredToOneRelationshipStrict(resourceType, relationshipName).getRelationship();
    }

    public RegisteredRelationship<ToOneRelationship<?, ?>> getRegisteredToOneRelationshipStrict(
            ResourceType resourceType,
                                                                                                RelationshipName relationshipName
    ) {
        Map<RelationshipName, RegisteredRelationship<ToOneRelationship<?, ?>>> resourceRelationships = this.toOneRelationships.get(resourceType);
        if (MapUtils.isEmpty(resourceRelationships)) {
            throw new DomainMisconfigurationException("No To One relationships found for the Resource ("
                    + resourceType.getType() + ").");
        }
        RegisteredRelationship<ToOneRelationship<?, ?>> result = resourceRelationships.get(relationshipName);
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

        private final List<JsonApi4jPlugin> plugins;

        private final Map<ResourceType, RegisteredResource<Resource<?>>> resources;
        private final Map<ResourceType, Map<RelationshipName, Relationship<?, ?>>> allRelationships;
        private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToOneRelationship<?, ?>>>> toOneRelationships;
        private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToManyRelationship<?, ?>>>> toManyRelationships;


        private DomainRegistryBuilder(List<JsonApi4jPlugin> plugins) {
            this.plugins = plugins;

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
            this.resources.put(resource.resourceType(), enrichWithPluginInfo(resource));
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
                ).put(relationship.relationshipName(), enrichWithPluginInfo(rel));
            }
            if (relationship instanceof ToManyRelationship<?, ?> rel) {
                this.toManyRelationships.computeIfAbsent(
                        relationship.resourceType(),
                        k -> new HashMap<>()
                ).put(relationship.relationshipName(), enrichWithPluginInfo(rel));
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

        private RegisteredResource<Resource<?>> enrichWithPluginInfo(Resource<?> resource) {
            Map<String, Object> pluginsInfo = new HashMap<>();
            for (JsonApi4jPlugin plugin : this.plugins) {
                Object pluginInfo = plugin.extractPluginInfoFromResource(resource);
                if (pluginInfo != null) {
                    pluginsInfo.put(plugin.pluginName(), pluginInfo);
                }
            }
            return RegisteredResource.builder()
                    .resource(resource)
                    .pluginInfo(Collections.unmodifiableMap(pluginsInfo))
                    .build();
        }

        private <T extends Relationship<?, ?>> RegisteredRelationship<T> enrichWithPluginInfo(T relationship) {
            Map<String, Object> pluginsInfo = new HashMap<>();
            for (JsonApi4jPlugin plugin : this.plugins) {
                Object pluginInfo = plugin.extractPluginInfoFromRelationship(relationship);
                if (pluginInfo != null) {
                    pluginsInfo.put(plugin.pluginName(), pluginInfo);
                }
            }
            return RegisteredRelationship.<T>builder()
                    .relationship(relationship)
                    .pluginInfo(Collections.unmodifiableMap(pluginsInfo))
                    .build();
        }
    }

}
