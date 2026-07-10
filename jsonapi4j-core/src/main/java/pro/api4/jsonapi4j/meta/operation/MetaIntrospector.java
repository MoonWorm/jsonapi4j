package pro.api4.jsonapi4j.meta.operation;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain;
import pro.api4.jsonapi4j.domain.RegisteredRelationship;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.meta.context.MetaRuntime;
import pro.api4.jsonapi4j.meta.domain.config.ConfigResource;
import pro.api4.jsonapi4j.meta.domain.config.ConfigResource.ConfigAttributes;
import pro.api4.jsonapi4j.meta.domain.operations.OperationsResource;
import pro.api4.jsonapi4j.meta.domain.operations.OperationsResource.OperationDescriptorAttributes;
import pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource;
import pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource.PluginAttributes;
import pro.api4.jsonapi4j.meta.domain.relationships.RelationshipsResource;
import pro.api4.jsonapi4j.meta.domain.relationships.RelationshipsResource.RelationshipDescriptorAttributes;
import pro.api4.jsonapi4j.meta.domain.resources.ResourcesResource;
import pro.api4.jsonapi4j.meta.domain.resources.ResourcesResource.ResourceDescriptorAttributes;
import pro.api4.jsonapi4j.meta.domain.state.StateResource.StateAttributes;
import pro.api4.jsonapi4j.meta.operation.config.ConfigIntrospector;
import pro.api4.jsonapi4j.meta.operation.operations.OperationsIntrospector;
import pro.api4.jsonapi4j.meta.operation.plugins.PluginsIntrospector;
import pro.api4.jsonapi4j.meta.operation.relationships.RelationshipsIntrospector;
import pro.api4.jsonapi4j.meta.operation.resources.ResourcesIntrospector;
import pro.api4.jsonapi4j.meta.operation.state.StateIntrospector;
import pro.api4.jsonapi4j.operation.OperationMeta;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.RegisteredOperation;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static pro.api4.jsonapi4j.domain.DomainRegistry.MetaDomain.isMetaResource;
import static pro.api4.jsonapi4j.meta.domain.operations.OperationsResource.operationId;
import static pro.api4.jsonapi4j.meta.domain.plugins.PluginsResource.pluginId;
import static pro.api4.jsonapi4j.meta.domain.relationships.RelationshipsResource.relationshipId;
import static pro.api4.jsonapi4j.meta.domain.resources.ResourcesResource.resourceId;

/**
 * Read-only projection of the live registries and {@link MetaContext} into the {@code *Attributes} records
 * that back the meta resources. Stateless apart from the {@link MetaRuntime} it reads through, so a single instance
 * is shared by every meta resource/relationship/operation.
 */
public class MetaIntrospector implements PluginsIntrospector,
        ResourcesIntrospector,
        RelationshipsIntrospector,
        OperationsIntrospector,
        StateIntrospector,
        ConfigIntrospector {

    private final MetaRuntime runtime;

    public MetaIntrospector(MetaRuntime runtime) {
        this.runtime = runtime;
    }

    @Override
    public List<PluginAttributes> plugins() {
        return runtime.getPlugins().stream()
                .map(p -> new PluginAttributes(
                        p.pluginName(),
                        p.enabled(),
                        p.precedence(),
                        p.getClass().getName()))
                .sorted(Comparator.comparing(PluginAttributes::name))
                .toList();
    }

    @Override
    public Optional<PluginAttributes> pluginById(String id) {
        return plugins().stream().filter(p -> pluginId(p).equals(id)).findFirst();
    }

    @Override
    public List<Ref> pluginRefs() {
        return plugins().stream().map(p -> new Ref(PluginsResource.PLUGINS, pluginId(p))).toList();
    }

    @Override
    public List<ResourceDescriptorAttributes> resources() {
        return runtime.getDomainRegistry().getResources().stream()
                .filter(r -> !isMetaResource(r.getResourceType()))
                .map(r -> new ResourceDescriptorAttributes(
                        r.getResourceType().getType(),
                        r.getRegisteredAs() != null ? r.getRegisteredAs().getName() : r.getResource().getClass().getName()))
                .sorted(Comparator.comparing(ResourceDescriptorAttributes::type))
                .toList();
    }

    @Override
    public Optional<ResourceDescriptorAttributes> resourceById(String id) {
        return resources().stream().filter(r -> resourceId(r).equals(id)).findFirst();
    }

    @Override
    public List<Ref> resourceRefs() {
        return resources().stream().map(r -> new Ref(ResourcesResource.RESOURCES, resourceId(r))).toList();
    }

    @Override
    public List<RelationshipDescriptorAttributes> relationships() {
        DomainRegistry domainRegistry = runtime.getDomainRegistry();
        return domainRegistry.getResourceTypes().stream()
                .flatMap(rt -> Stream.concat(
                        domainRegistry.getToOneRelationships(rt).stream(),
                        domainRegistry.getToManyRelationships(rt).stream()))
                .filter(rr -> !isMetaResource(rr.getParentResourceType()))
                .map(this::toRelationshipAttributes)
                .sorted(Comparator.comparing(RelationshipsResource::relationshipId))
                .toList();
    }

    @Override
    public Optional<RelationshipDescriptorAttributes> relationshipById(String id) {
        return relationships().stream()
                .filter(r -> relationshipId(r).equals(id))
                .findFirst();
    }

    @Override
    public List<Ref> relationshipRefs() {
        return relationships().stream()
                .map(r -> new Ref(RelationshipsResource.RELATIONSHIPS, relationshipId(r)))
                .toList();
    }

    private RelationshipDescriptorAttributes toRelationshipAttributes(RegisteredRelationship<? extends Relationship<?>> rr) {
        return new RelationshipDescriptorAttributes(
                rr.getRelationshipName().getName(),
                rr.getParentResourceType().getType(),
                rr.getRelationshipType().name(),
                rr.getRelationship().getClass().getName());
    }

    @Override
    public List<OperationDescriptorAttributes> operations() {
        return runtime.getOperationsRegistry().getAllRegisteredOperations().stream()
                .filter(ro -> !isMetaResource(ro.getOperationMeta().getResourceType()))
                .map(this::toOperationAttributes)
                .sorted(Comparator.comparing(OperationsResource::operationId))
                .toList();
    }

    @Override
    public Optional<OperationDescriptorAttributes> operationById(String id) {
        return operations().stream()
                .filter(o -> operationId(o).equals(id))
                .findFirst();
    }

    @Override
    public List<Ref> operationRefs() {
        return operations().stream()
                .map(o -> new Ref(OperationsResource.OPERATIONS, operationId(o)))
                .toList();
    }

    private OperationDescriptorAttributes toOperationAttributes(RegisteredOperation<?> ro) {
        OperationMeta meta = ro.getOperationMeta();
        String resourceType = meta.getResourceType().getType();
        String relationshipName = meta.getRelationshipName() != null ? meta.getRelationshipName().getName() : null;
        OperationType operationType = meta.getOperationType();
        return new OperationDescriptorAttributes(
                operationType.name(),
                operationType.getMethod().name(),
                pathTemplate(resourceType, relationshipName, operationType),
                resourceType,
                relationshipName,
                ro.getOperation().getClass().getName());
    }

    private String pathTemplate(String resourceType, String relationshipName, OperationType operationType) {
        String rootPath = runtime.getContext().getRootPath();
        return switch (operationType) {
            case READ_MULTIPLE_RESOURCES, CREATE_RESOURCE -> String.format("%s/%s", rootPath, resourceType);
            case READ_RESOURCE_BY_ID, UPDATE_RESOURCE, DELETE_RESOURCE ->
                    String.format("%s/%s/{id}", rootPath, resourceType);
            default -> String.format("%s/%s/{id}/relationships/%s", rootPath, resourceType, relationshipName);
        };
    }

    @Override
    public StateAttributes state() {
        MetaContext ctx = runtime.getContext();
        return new StateAttributes(
                ctx.getFrameworkVersion(),
                ctx.getJavaVersion(),
                ctx.getIntegration() != null ? ctx.getIntegration().name() : null,
                plugins().size(),
                resources().size(),
                relationships().size(),
                operations().size());
    }

    @Override
    public ConfigAttributes config() {
        return new ConfigAttributes(runtime.getContext().getConfig());
    }

    @Override
    public Ref configRef() {
        return new Ref(ConfigResource.CONFIG, MetaDomain.SINGLETON_ID);
    }

}
