package pro.api4.jsonapi4j.domain;


import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.processor.RelationshipType;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.domain.annotation.JsonApiDomainAnnotationsUtil.resolveRelationshipName;
import static pro.api4.jsonapi4j.domain.annotation.JsonApiDomainAnnotationsUtil.resolveResourceType;

public class DomainRegistry {

    private final Map<ResourceType, RegisteredResource<Resource<?>>> resources;
    private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<? extends Relationship<?>>>> allRelationships;
    private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToOneRelationship<?>>>> toOneRelationships;
    private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToManyRelationship<?>>>> toManyRelationships;

    private final Map<Class<?>, RegisteredResource<Resource<?>>> resourcesByClass;
    private final Map<Class<?>, RegisteredRelationship<Relationship<?>>> relationshipsByClass;

    private DomainRegistry(
            Map<ResourceType, RegisteredResource<Resource<?>>> resources,
            Map<ResourceType, Map<RelationshipName, RegisteredRelationship<? extends Relationship<?>>>> allRelationships,
            Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToOneRelationship<?>>>> toOneRelationships,
            Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToManyRelationship<?>>>> toManyRelationships,
            Map<Class<?>, RegisteredResource<Resource<?>>> resourcesByClass,
            Map<Class<?>, RegisteredRelationship<Relationship<?>>> relationshipsByClass
    ) {
        this.resources = MapUtils.emptyIfNull(resources);
        this.allRelationships = MapUtils.emptyIfNull(allRelationships);
        this.toOneRelationships = MapUtils.emptyIfNull(toOneRelationships);
        this.toManyRelationships = MapUtils.emptyIfNull(toManyRelationships);
        this.resourcesByClass = MapUtils.emptyIfNull(resourcesByClass);
        this.relationshipsByClass = MapUtils.emptyIfNull(relationshipsByClass);
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

    public RegisteredResource<Resource<?>> getResource(ResourceType resourceType) {
        return this.resources.get(resourceType);
    }

    public RegisteredResource<Resource<?>> getResource(Class<?> registeredAs) {
        return this.resourcesByClass.get(registeredAs);
    }

    public Collection<RegisteredResource<Resource<?>>> getResources() {
        return Collections.unmodifiableCollection(this.resources.values());
    }

    public Set<ResourceType> getResourceTypes() {
        return Collections.unmodifiableSet(this.resources.keySet());
    }

    public List<RegisteredRelationship<ToManyRelationship<?>>> getToManyRelationships(ResourceType resourceType) {
        return MapUtils.emptyIfNull(this.toManyRelationships.get(resourceType))
                .values()
                .stream()
                .toList();
    }

    public Set<RelationshipName> getToManyRelationshipNames(ResourceType resourceType) {
        return getToManyRelationships(resourceType)
                .stream()
                .map(RegisteredRelationship::getRelationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public List<RegisteredRelationship<ToOneRelationship<?>>> getToOneRelationships(
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
                .map(RegisteredRelationship::getRelationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    public RegisteredRelationship<ToManyRelationship<?>> getToManyRelationshipStrict(
            ResourceType resourceType,
            RelationshipName relationshipName
    ) {
        Map<RelationshipName, RegisteredRelationship<ToManyRelationship<?>>> resourceRelationships = this.toManyRelationships.get(resourceType);
        if (MapUtils.isEmpty(resourceRelationships)) {
            throw new DomainMisconfigurationException("No To Many relationships found for the Resource ("
                    + resourceType.getType() + ").");
        }
        RegisteredRelationship<ToManyRelationship<?>> result = resourceRelationships.get(relationshipName);
        if (result == null) {
            throw new DomainMisconfigurationException("Implementation of the '"
                    + relationshipName.getName() + "' To Many Relationship is not found for the Resource ("
                    + resourceType.getType() + "). Please implement the relationship.");
        }
        return result;
    }

    public RegisteredRelationship<ToOneRelationship<?>> getToOneRelationshipStrict(
            ResourceType resourceType,
            RelationshipName relationshipName
    ) {
        Map<RelationshipName, RegisteredRelationship<ToOneRelationship<?>>> resourceRelationships = this.toOneRelationships.get(resourceType);
        if (MapUtils.isEmpty(resourceRelationships)) {
            throw new DomainMisconfigurationException("No To One relationships found for the Resource ("
                    + resourceType.getType() + ").");
        }
        RegisteredRelationship<ToOneRelationship<?>> result = resourceRelationships.get(relationshipName);
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
                .map(RegisteredRelationship::getRelationshipName)
                .collect(Collectors.toUnmodifiableSet());
    }

    @Slf4j
    public static class DomainRegistryBuilder {

        private final List<JsonApi4jPlugin> plugins;

        private final Map<ResourceType, RegisteredResource<Resource<?>>> resources;
        private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<? extends Relationship<?>>>> allRelationships;
        private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToOneRelationship<?>>>> toOneRelationships;
        private final Map<ResourceType, Map<RelationshipName, RegisteredRelationship<ToManyRelationship<?>>>> toManyRelationships;

        private final Map<Class<?>, RegisteredResource<Resource<?>>> resourcesByClass;
        private final Map<Class<?>, RegisteredRelationship<Relationship<?>>> relationshipsByClass;

        private DomainRegistryBuilder(List<JsonApi4jPlugin> plugins) {
            this.plugins = plugins;

            this.resources = new HashMap<>();
            this.allRelationships = new HashMap<>();
            this.toOneRelationships = new HashMap<>();
            this.toManyRelationships = new HashMap<>();

            this.resourcesByClass = new HashMap<>();
            this.relationshipsByClass = new HashMap<>();
        }

        public DomainRegistryBuilder resources(Set<Resource<?>> resources) {
            for (Resource<?> resource : resources) {
                this.resource(resource);
            }
            return this;
        }

        public DomainRegistryBuilder resource(Resource<?> resource) {
            RegisteredResource<Resource<?>> registeredResource = enrichWithMetaInfo(resource);
            ResourceType resourceType = registeredResource.getResourceType();
            if (this.resources.containsKey(resourceType)) {
                throw new DomainMisconfigurationException("Multiple resource declarations found for : "
                        + resourceType + " resource type");
            }
            this.resources.put(resourceType, registeredResource);
            this.resourcesByClass.put(registeredResource.getRegisteredAs(), registeredResource);
            log.info("{} resource of '{}' type has been registered.", resource.getClass().getSimpleName(), resourceType);
            return this;
        }

        public DomainRegistryBuilder relationships(Set<Relationship<?>> relationships) {
            for (Relationship<?> relationship : relationships) {
                this.relationship(relationship);
            }
            return this;
        }

        public DomainRegistryBuilder relationship(Relationship<?> relationship) {
            // pre-filter relationship by type
            if (relationship instanceof ToOneRelationship<?> rel) {
                RegisteredRelationship<ToOneRelationship<?>> rr = enrichWithMetaInfo(rel);
                registerRelationship(rr, (r) ->
                        this.toOneRelationships.computeIfAbsent(
                                r.getParentResourceType(),
                                k -> new HashMap<>()
                        ).put(r.getRelationshipName(), r)
                );

            }
            if (relationship instanceof ToManyRelationship<?> rel) {
                RegisteredRelationship<ToManyRelationship<?>> rr = enrichWithMetaInfo(rel);
                registerRelationship(rr, (r) ->
                        this.toManyRelationships.computeIfAbsent(
                                r.getParentResourceType(),
                                k -> new HashMap<>()
                        ).put(r.getRelationshipName(), r)
                );
            }
            return this;
        }

        private <T extends Relationship<?>> void registerRelationship(RegisteredRelationship<T> rr,
                                                                         Consumer<RegisteredRelationship<T>> registerRelationshipConsumer) {
            ResourceType resourceType = rr.getParentResourceType();
            RelationshipName relationshipName = rr.getRelationshipName();
            if (this.allRelationships.containsKey(resourceType)
                    && this.allRelationships.get(resourceType).containsKey(relationshipName)) {
                throw new DomainMisconfigurationException("Multiple relationship declarations found for : "
                        + resourceType + " resource type, for relationship: " + relationshipName);
            }
            registerRelationshipConsumer.accept(rr);
            this.allRelationships.computeIfAbsent(
                    resourceType,
                    k -> new HashMap<>()
            ).put(
                    relationshipName,
                    rr
            );
            log.info(
                    "{} relationship with '{}' name has been registered for '{}' type as {} Relationship.",
                    rr.getRelationship().getClass().getSimpleName(),
                    relationshipName,
                    resourceType,
                    rr.getRelationshipType().name()
            );
        }

        public DomainRegistry build() {
            return new DomainRegistry(
                    Collections.unmodifiableMap(this.resources),
                    Collections.unmodifiableMap(this.allRelationships),
                    Collections.unmodifiableMap(this.toOneRelationships),
                    Collections.unmodifiableMap(this.toManyRelationships),
                    Collections.unmodifiableMap(this.resourcesByClass),
                    Collections.unmodifiableMap(this.relationshipsByClass)
            );
        }

        private RegisteredResource<Resource<?>> enrichWithMetaInfo(Resource<?> resource) {
            Map<String, Object> pluginsInfo = new HashMap<>();
            for (JsonApi4jPlugin plugin : this.plugins) {
                Object pluginInfo = plugin.extractPluginInfoFromResource(resource);
                if (pluginInfo != null) {
                    pluginsInfo.put(plugin.pluginName(), pluginInfo);
                }
            }
            return RegisteredResource.builder()
                    .resource(resource)
                    .resourceType(resolveResourceType(resource.getClass()))
                    .registeredAs(resource.getClass())
                    .pluginInfo(Collections.unmodifiableMap(pluginsInfo))
                    .build();
        }

        private <T extends Relationship<?>> RegisteredRelationship<T> enrichWithMetaInfo(T relationship) {
            JsonApiRelationship jsonApiRelationship = relationship.getClass().getAnnotation(JsonApiRelationship.class);
            if (jsonApiRelationship == null) {
                throw new DomainMisconfigurationException("Each relationship implementation must has " + JsonApiRelationship.class.getSimpleName() + " annotation placed on the type level.");
            }
            ResourceType parentResourceType = resolveResourceType(jsonApiRelationship.parentResource());
            RelationshipType relationshipType = relationship instanceof ToOneRelationship<?>
                    ? RelationshipType.TO_ONE
                    : RelationshipType.TO_MANY;

            Map<String, Object> pluginsInfo = new HashMap<>();
            for (JsonApi4jPlugin plugin : this.plugins) {
                Object pluginInfo = plugin.extractPluginInfoFromRelationship(relationship);
                if (pluginInfo != null) {
                    pluginsInfo.put(plugin.pluginName(), pluginInfo);
                }
            }
            return RegisteredRelationship.<T>builder()
                    .relationship(relationship)
                    .parentResourceType(parentResourceType)
                    .relationshipName(resolveRelationshipName(relationship.getClass()))
                    .relationshipType(relationshipType)
                    .pluginInfo(Collections.unmodifiableMap(pluginsInfo))
                    .build();
        }
    }

}
