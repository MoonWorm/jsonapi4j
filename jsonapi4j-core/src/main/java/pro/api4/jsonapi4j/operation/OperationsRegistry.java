package pro.api4.jsonapi4j.operation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.operation.exception.OperationsMisconfigurationException;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pro.api4.jsonapi4j.operation.OperationType.*;

public class OperationsRegistry {

    private final Map<ResourceType, RegisteredOperation<ReadResourceByIdOperation<?>>> readResourceByIdOperations;
    private final Map<ResourceType, RegisteredOperation<ReadMultipleResourcesOperation<?>>> readMultipleResourcesOperations;
    private final Map<ResourceType, RegisteredOperation<CreateResourceOperation<?>>> createResourceOperations;
    private final Map<ResourceType, RegisteredOperation<UpdateResourceOperation>> updateResourceOperations;
    private final Map<ResourceType, RegisteredOperation<DeleteResourceOperation>> deleteResourceOperations;
    private final Map<ResourceType, Map<RelationshipName, RegisteredOperation<ReadToOneRelationshipOperation<?, ?>>>> readToOneRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, RegisteredOperation<ReadToManyRelationshipOperation<?, ?>>>> readToManyRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, RegisteredOperation<UpdateToOneRelationshipOperation>>> updateToOneRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, RegisteredOperation<UpdateToManyRelationshipOperation>>> updateToManyRelationshipOperations;
    private final Set<ResourceType> resourceTypesWithAnyOperationConfigured;
    private final Map<ResourceType, Set<RelationshipName>> relationshipNamesWithAnyOperationConfigured;

    private OperationsRegistry(Map<ResourceType, RegisteredOperation<ReadResourceByIdOperation<?>>> readResourceByIdOperations,
                               Map<ResourceType, RegisteredOperation<ReadMultipleResourcesOperation<?>>> readMultipleResourcesOperations,
                               Map<ResourceType, RegisteredOperation<CreateResourceOperation<?>>> createResourceOperations,
                               Map<ResourceType, RegisteredOperation<UpdateResourceOperation>> updateResourceOperations,
                               Map<ResourceType, RegisteredOperation<DeleteResourceOperation>> deleteResourceOperations,
                               Map<ResourceType, Map<RelationshipName, RegisteredOperation<ReadToOneRelationshipOperation<?, ?>>>> readToOneRelationshipOperations,
                               Map<ResourceType, Map<RelationshipName, RegisteredOperation<ReadToManyRelationshipOperation<?, ?>>>> readToManyRelationshipOperations,
                               Map<ResourceType, Map<RelationshipName, RegisteredOperation<UpdateToOneRelationshipOperation>>> updateToOneRelationshipOperations,
                               Map<ResourceType, Map<RelationshipName, RegisteredOperation<UpdateToManyRelationshipOperation>>> updateToManyRelationshipOperations,
                               Set<ResourceType> resourceTypesWithAnyOperationConfigured,
                               Map<ResourceType, Set<RelationshipName>> relationshipNamesWithAnyOperationConfigured) {
        this.readResourceByIdOperations = readResourceByIdOperations;
        this.readMultipleResourcesOperations = readMultipleResourcesOperations;
        this.createResourceOperations = createResourceOperations;
        this.updateResourceOperations = updateResourceOperations;
        this.deleteResourceOperations = deleteResourceOperations;
        this.readToOneRelationshipOperations = readToOneRelationshipOperations;
        this.readToManyRelationshipOperations = readToManyRelationshipOperations;
        this.updateToOneRelationshipOperations = updateToOneRelationshipOperations;
        this.updateToManyRelationshipOperations = updateToManyRelationshipOperations;
        this.resourceTypesWithAnyOperationConfigured = resourceTypesWithAnyOperationConfigured;
        this.relationshipNamesWithAnyOperationConfigured = relationshipNamesWithAnyOperationConfigured;
    }

    public static OperationsRegistryBuilder builder(List<JsonApi4jPlugin> plugins) {
        return new OperationsRegistryBuilder(plugins);
    }

    public static OperationsRegistry empty() {
        return builder(Collections.emptyList()).build();
    }

    @SuppressWarnings("LombokGetterMayBeUsed")
    public Set<ResourceType> getResourceTypesWithAnyOperationConfigured() {
        return this.resourceTypesWithAnyOperationConfigured;
    }

    public Set<RelationshipName> getRelationshipNamesWithAnyOperationConfigured(ResourceType resourceType) {
        return SetUtils.emptyIfNull(this.relationshipNamesWithAnyOperationConfigured.get(resourceType));
    }

    public ResourceOperation getResourceOperation(ResourceType resourceType,
                                                  OperationType operationType,
                                                  boolean orElseThrow) {
        RegisteredOperation<? extends ResourceOperation> registeredOperation
                = getRegisteredResourceOperation(resourceType, operationType, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<? extends ResourceOperation> getRegisteredResourceOperation(
            ResourceType resourceType,
            OperationType operationType,
            boolean orElseThrow
    ) {
        if (operationType.getSubType() == OperationType.SubType.RESOURCE) {
            if (operationType == READ_RESOURCE_BY_ID) {
                return getRegisteredReadResourceByIdOperation(resourceType, orElseThrow);
            } else if (operationType == READ_MULTIPLE_RESOURCES) {
                return getRegisteredReadMultipleResourcesOperation(resourceType, orElseThrow);
            } else if (operationType == CREATE_RESOURCE) {
                return getRegisteredCreateResourceOperation(resourceType, orElseThrow);
            } else if (operationType == UPDATE_RESOURCE) {
                return getRegisteredUpdateResourceOperation(resourceType, orElseThrow);
            } else {
                return getRegisteredDeleteResourceOperation(resourceType, orElseThrow);
            }
        } else {
            if (orElseThrow) {
                throw new IllegalArgumentException("Unsupported operation type: " + operationType
                        + ". This operation type doesn't represent a resource operation");
            } else {
                return null;
            }
        }
    }

    public boolean isResourceOperationConfigured(ResourceType resourceType,
                                                 OperationType operationType) {
        return getResourceOperation(resourceType, operationType, false) != null;
    }

    public boolean isAnyResourceOperationConfigured(ResourceType resourceType) {
        return OperationType.getResourceOperationTypes()
                .stream()
                .anyMatch(operationType -> isResourceOperationConfigured(resourceType, operationType));
    }

    public RelationshipOperation getRelationshipOperation(ResourceType resourceType,
                                                          RelationshipName relationshipName,
                                                          OperationType operationType,
                                                          boolean orElseThrow) {
        RegisteredOperation<? extends RelationshipOperation> registeredOperation
                = getRegisteredRelationshipOperation(resourceType, relationshipName, operationType, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<? extends RelationshipOperation> getRegisteredRelationshipOperation(
            ResourceType resourceType,
            RelationshipName relationshipName,
            OperationType operationType,
            boolean orElseThrow
    ) {
        if (operationType.getSubType() == OperationType.SubType.TO_ONE_RELATIONSHIP) {
            if (operationType == READ_TO_ONE_RELATIONSHIP) {
                return getRegisteredReadToOneRelationshipOperation(resourceType, relationshipName, orElseThrow);
            } else {
                return getRegisteredUpdateToOneRelationshipOperation(resourceType, relationshipName, orElseThrow);
            }
        } else if (operationType.getSubType() == OperationType.SubType.TO_MANY_RELATIONSHIP) {
            if (operationType == READ_TO_MANY_RELATIONSHIP) {
                return getRegisteredReadToManyRelationshipOperation(resourceType, relationshipName, orElseThrow);
            } else {
                return getRegisteredUpdateToManyRelationshipOperation(resourceType, relationshipName, orElseThrow);
            }
        } else {
            if (orElseThrow) {
                throw new IllegalArgumentException("Unsupported operation type: " + operationType + ". This operation type doesn't represent a relationship operation");
            } else {
                return null;
            }
        }
    }

    public boolean isRelationshipOperationConfigured(ResourceType resourceType,
                                                     RelationshipName relationshipName,
                                                     OperationType operationType) {
        return getRelationshipOperation(resourceType, relationshipName, operationType, false) != null;
    }

    public boolean isToManyRelationshipOperationConfigured(ResourceType resourceType,
                                                           RelationshipName relationshipName,
                                                           OperationType operationType) {
        return operationType.getSubType() == OperationType.SubType.TO_MANY_RELATIONSHIP
                && getRelationshipOperation(resourceType, relationshipName, operationType, false) != null;
    }

    public boolean isAnyToManyRelationshipOperationConfigured(ResourceType resourceType,
                                                              RelationshipName relationshipName) {
        return OperationType.getToManyRelationshipOperationTypes()
                .stream()
                .anyMatch(operationType -> isToManyRelationshipOperationConfigured(resourceType, relationshipName, operationType));
    }

    public boolean isAnyToManyRelationshipOperationConfigured(ResourceType resourceType) {
        return OperationType.getToManyRelationshipOperationTypes()
                .stream()
                .anyMatch(operationType ->
                        readToManyRelationshipOperations.containsKey(resourceType)
                                || updateToManyRelationshipOperations.containsKey(resourceType));
    }

    public boolean isToOneRelationshipOperationConfigured(ResourceType resourceType,
                                                          RelationshipName relationshipName,
                                                          OperationType operationType) {
        return operationType.getSubType() == OperationType.SubType.TO_ONE_RELATIONSHIP
                && getRelationshipOperation(resourceType, relationshipName, operationType, false) != null;
    }

    public boolean isAnyToOneRelationshipOperationConfigured(ResourceType resourceType,
                                                             RelationshipName relationshipName) {
        return OperationType.getToOneRelationshipOperationTypes()
                .stream()
                .anyMatch(operationType -> isToOneRelationshipOperationConfigured(resourceType, relationshipName, operationType));
    }

    public boolean isAnyToOneRelationshipOperationConfigured(ResourceType resourceType) {
        return OperationType.getToOneRelationshipOperationTypes()
                .stream()
                .anyMatch(operationType ->
                        readToOneRelationshipOperations.containsKey(resourceType)
                                || updateToOneRelationshipOperations.containsKey(resourceType));
    }

    public ReadResourceByIdOperation<?> getReadResourceByIdOperation(ResourceType resourceType,
                                                                     boolean orElseThrow) {
        RegisteredOperation<ReadResourceByIdOperation<?>> registeredOperation
                = getRegisteredReadResourceByIdOperation(resourceType, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<ReadResourceByIdOperation<?>> getRegisteredReadResourceByIdOperation(
            ResourceType resourceType,
            boolean orElseThrow
    ) {
        if (!readResourceByIdOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(READ_RESOURCE_BY_ID, resourceType);
            }
            return null;
        }
        return readResourceByIdOperations.get(resourceType);
    }

    public ReadMultipleResourcesOperation<?> getReadMultipleResourcesOperation(ResourceType resourceType,
                                                                               boolean orElseThrow) {
        RegisteredOperation<ReadMultipleResourcesOperation<?>> registeredOperation
                = getRegisteredReadMultipleResourcesOperation(resourceType, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<ReadMultipleResourcesOperation<?>> getRegisteredReadMultipleResourcesOperation(
            ResourceType resourceType,
            boolean orElseThrow
    ) {
        if (!readMultipleResourcesOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(READ_MULTIPLE_RESOURCES, resourceType);
            }
            return null;
        }
        return readMultipleResourcesOperations.get(resourceType);
    }

    public CreateResourceOperation<?> getCreateResourceOperation(ResourceType resourceType,
                                                                 boolean orElseThrow) {
        RegisteredOperation<CreateResourceOperation<?>> registeredOperation
                = getRegisteredCreateResourceOperation(resourceType, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<CreateResourceOperation<?>> getRegisteredCreateResourceOperation(
            ResourceType resourceType,
            boolean orElseThrow
    ) {
        if (!createResourceOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(CREATE_RESOURCE, resourceType);
            }
            return null;
        }
        return createResourceOperations.get(resourceType);
    }

    public UpdateResourceOperation getUpdateResourceOperation(ResourceType resourceType,
                                                              boolean orElseThrow) {
        RegisteredOperation<UpdateResourceOperation> registeredOperation
                = getRegisteredUpdateResourceOperation(resourceType, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<UpdateResourceOperation> getRegisteredUpdateResourceOperation(
            ResourceType resourceType,
            boolean orElseThrow
    ) {
        if (!updateResourceOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(UPDATE_RESOURCE, resourceType);
            }
            return null;
        }
        return updateResourceOperations.get(resourceType);
    }

    public DeleteResourceOperation getDeleteResourceOperation(ResourceType resourceType,
                                                              boolean orElseThrow) {
        RegisteredOperation<DeleteResourceOperation> registeredOperation
                = getRegisteredDeleteResourceOperation(resourceType, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<DeleteResourceOperation> getRegisteredDeleteResourceOperation(
            ResourceType resourceType,
            boolean orElseThrow
    ) {
        if (!deleteResourceOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(DELETE_RESOURCE, resourceType);
            }
            return null;
        }
        return deleteResourceOperations.get(resourceType);
    }

    public ReadToOneRelationshipOperation<?, ?> getReadToOneRelationshipOperation(ResourceType resourceType,
                                                                                  RelationshipName relationshipName,
                                                                                  boolean orElseThrow) {
        RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> registeredOperation
                = getRegisteredReadToOneRelationshipOperation(resourceType, relationshipName, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> getRegisteredReadToOneRelationshipOperation(
            ResourceType resourceType,
            RelationshipName relationshipName,
            boolean orElseThrow
    ) {
        if (!readToOneRelationshipOperations.containsKey(resourceType)
                || !readToOneRelationshipOperations.get(resourceType).containsKey(relationshipName)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(READ_TO_ONE_RELATIONSHIP, resourceType, relationshipName);
            }
            return null;
        }
        return readToOneRelationshipOperations.get(resourceType).get(relationshipName);
    }

    public ReadToManyRelationshipOperation<?, ?> getReadToManyRelationshipOperation(ResourceType resourceType,
                                                                                    RelationshipName relationshipName,
                                                                                    boolean orElseThrow) {
        RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> registeredOperation
                = getRegisteredReadToManyRelationshipOperation(resourceType, relationshipName, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> getRegisteredReadToManyRelationshipOperation(
            ResourceType resourceType,
            RelationshipName relationshipName,
            boolean orElseThrow
    ) {
        if (!readToManyRelationshipOperations.containsKey(resourceType)
                || !readToManyRelationshipOperations.get(resourceType).containsKey(relationshipName)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(READ_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
            }
            return null;
        }
        return readToManyRelationshipOperations.get(resourceType).get(relationshipName);
    }

    public Set<RelationshipName> getRelationshipNamesWithReadOperationConfigured(ResourceType resourceType) {
        return SetUtils.union(
                MapUtils.emptyIfNull(readToManyRelationshipOperations.get(resourceType)).keySet(),
                MapUtils.emptyIfNull(readToOneRelationshipOperations.get(resourceType)).keySet()
        );
    }

    public List<UpdateToOneRelationshipOperation> getUpdateToOneRelationshipOperations(ResourceType resourceType) {
        return getRegisteredUpdateToOneRelationshipOperations(resourceType)
                .stream()
                .map(RegisteredOperation::getOperation)
                .toList();
    }

    public List<RegisteredOperation<UpdateToOneRelationshipOperation>> getRegisteredUpdateToOneRelationshipOperations(
            ResourceType resourceType
    ) {
        return MapUtils.emptyIfNull(updateToOneRelationshipOperations.get(resourceType))
                .values()
                .stream()
                .toList();
    }

    public UpdateToOneRelationshipOperation getUpdateToOneRelationshipOperation(ResourceType resourceType,
                                                                                RelationshipName relationshipName,
                                                                                boolean orElseThrow) {
        RegisteredOperation<UpdateToOneRelationshipOperation> registeredOperation
                = getRegisteredUpdateToOneRelationshipOperation(resourceType, relationshipName, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<UpdateToOneRelationshipOperation> getRegisteredUpdateToOneRelationshipOperation(
            ResourceType resourceType,
            RelationshipName relationshipName,
            boolean orElseThrow
    ) {
        if (!updateToOneRelationshipOperations.containsKey(resourceType)
                || !updateToOneRelationshipOperations.get(resourceType).containsKey(relationshipName)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(UPDATE_TO_ONE_RELATIONSHIP, resourceType, relationshipName);
            }
            return null;
        }
        return updateToOneRelationshipOperations.get(resourceType).get(relationshipName);
    }

    public UpdateToManyRelationshipOperation getUpdateToManyRelationshipOperation(ResourceType resourceType,
                                                                                  RelationshipName relationshipName,
                                                                                  boolean orElseThrow) {
        RegisteredOperation<UpdateToManyRelationshipOperation> registeredOperation
                = getRegisteredUpdateToManyRelationshipOperation(resourceType, relationshipName, orElseThrow);
        if (registeredOperation != null) {
            return registeredOperation.getOperation();
        }
        return null;
    }

    public RegisteredOperation<UpdateToManyRelationshipOperation> getRegisteredUpdateToManyRelationshipOperation(
            ResourceType resourceType,
            RelationshipName relationshipName,
            boolean orElseThrow
    ) {
        if (!updateToManyRelationshipOperations.containsKey(resourceType)
                || !updateToManyRelationshipOperations.get(resourceType).containsKey(relationshipName)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(UPDATE_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
            }
            return null;
        }
        return updateToManyRelationshipOperations.get(resourceType).get(relationshipName);
    }

    public List<UpdateToManyRelationshipOperation> getUpdateToManyRelationshipOperationsFor(ResourceType resourceType) {
        return getRegisteredUpdateToManyRelationshipOperationsFor(resourceType)
                .stream()
                .map(RegisteredOperation::getOperation)
                .toList();
    }

    public List<RegisteredOperation<UpdateToManyRelationshipOperation>> getRegisteredUpdateToManyRelationshipOperationsFor(
            ResourceType resourceType
    ) {
        return MapUtils.emptyIfNull(updateToManyRelationshipOperations.get(resourceType))
                .values()
                .stream()
                .toList();
    }

    public List<? extends Operation> getAllOperations() {
        return getAllRegisteredOperations().stream().map(RegisteredOperation::getOperation).toList();
    }

    private List<RegisteredOperation<? extends Operation>> getAllRegisteredOperations() {
        List<RegisteredOperation<? extends Operation>> result = new ArrayList<>();
        result.addAll(this.readResourceByIdOperations.values());
        result.addAll(this.readMultipleResourcesOperations.values());
        result.addAll(this.createResourceOperations.values());
        result.addAll(this.updateResourceOperations.values());
        result.addAll(this.deleteResourceOperations.values());
        this.readToOneRelationshipOperations.forEach((resourceType, relationshipOperations) -> {
            result.addAll(relationshipOperations.values());
        });
        this.readToManyRelationshipOperations.forEach((resourceType, relationshipOperations) -> {
            result.addAll(relationshipOperations.values());
        });
        this.updateToOneRelationshipOperations.forEach((resourceType, relationshipOperations) -> {
            result.addAll(relationshipOperations.values());
        });
        this.updateToManyRelationshipOperations.forEach((resourceType, relationshipOperations) -> {
            result.addAll(relationshipOperations.values());
        });
        return Collections.unmodifiableList(result);
    }

    public Object getPluginInfo(ResourceType resourceType,
                                OperationType operationType,
                                String pluginName) {
        return MapUtils.emptyIfNull(getPluginsInfo(resourceType, operationType)).get(pluginName);
    }

    public Map<String, Object> getPluginsInfo(ResourceType resourceType,
                                              OperationType operationType) {
        RegisteredOperation<? extends ResourceOperation> registeredOperation
                = getRegisteredResourceOperation(resourceType, operationType, false);
        if (registeredOperation != null) {
            return registeredOperation.getPluginInfo();
        }
        return null;
    }

    public Object getPluginInfo(ResourceType resourceType,
                                RelationshipName relationshipName,
                                OperationType operationType,
                                String pluginName) {
        return MapUtils.emptyIfNull(getPluginsInfo(resourceType, relationshipName, operationType)).get(pluginName);
    }

    public Map<String, Object> getPluginsInfo(ResourceType resourceType,
                                              RelationshipName relationshipName,
                                              OperationType operationType) {
        RegisteredOperation<? extends RelationshipOperation> registeredOperation
                = getRegisteredRelationshipOperation(resourceType, relationshipName, operationType, false);
        if (registeredOperation != null) {
            return registeredOperation.getPluginInfo();
        }
        return null;
    }

    @Slf4j
    public static class OperationsRegistryBuilder {

        private final List<JsonApi4jPlugin> plugins;

        private final Map<ResourceType, RegisteredOperation<ReadResourceByIdOperation<?>>> readResourceByIdOperations;
        private final Map<ResourceType, RegisteredOperation<ReadMultipleResourcesOperation<?>>> readMultipleResourcesOperations;
        private final Map<ResourceType, RegisteredOperation<CreateResourceOperation<?>>> createResourceOperations;
        private final Map<ResourceType, RegisteredOperation<UpdateResourceOperation>> updateResourceOperations;
        private final Map<ResourceType, RegisteredOperation<DeleteResourceOperation>> deleteResourceOperations;
        private final Map<ResourceType, Map<RelationshipName, RegisteredOperation<ReadToOneRelationshipOperation<?, ?>>>> readToOneRelationshipOperations;
        private final Map<ResourceType, Map<RelationshipName, RegisteredOperation<ReadToManyRelationshipOperation<?, ?>>>> readToManyRelationshipOperations;
        private final Map<ResourceType, Map<RelationshipName, RegisteredOperation<UpdateToOneRelationshipOperation>>> updateToOneRelationshipOperations;
        private final Map<ResourceType, Map<RelationshipName, RegisteredOperation<UpdateToManyRelationshipOperation>>> updateToManyRelationshipOperations;
        private final Set<ResourceType> resourceTypesWithAnyOperationConfigured;
        private final Map<ResourceType, Set<RelationshipName>> relationshipNamesWithAnyOperationConfigured;

        private OperationsRegistryBuilder(List<JsonApi4jPlugin> plugins) {
            this.plugins = plugins;

            this.readResourceByIdOperations = new HashMap<>();
            this.readMultipleResourcesOperations = new HashMap<>();
            this.createResourceOperations = new HashMap<>();
            this.updateResourceOperations = new HashMap<>();
            this.deleteResourceOperations = new HashMap<>();

            this.readToOneRelationshipOperations = new HashMap<>();
            this.readToManyRelationshipOperations = new HashMap<>();
            this.updateToOneRelationshipOperations = new HashMap<>();
            this.updateToManyRelationshipOperations = new HashMap<>();

            this.resourceTypesWithAnyOperationConfigured = new HashSet<>();
            this.relationshipNamesWithAnyOperationConfigured = new HashMap<>();
        }

        public OperationsRegistryBuilder repository(ResourceRepository<?> repository) {
            return this.operation(repository);
        }


        public OperationsRegistryBuilder repository(ToOneRelationshipRepository<?, ?> repository) {
            return this.operation(repository);
        }

        public OperationsRegistryBuilder repository(ToManyRelationshipRepository<?, ?> repository) {
            return this.operation(repository);
        }

        public OperationsRegistryBuilder operation(ResourceOperation operation) {
            Validate.notNull(operation);
            Set<RegisteredOperation<?>> registeredAs = new HashSet<>();
            if (operation instanceof ReadResourceByIdOperation<?> o) {
                RegisteredOperation<ReadResourceByIdOperation<?>> ro
                        = enrichWithMetaInfo(o, READ_RESOURCE_BY_ID, ReadResourceByIdOperation.class);
                readResourceByIdOperations.put(ro.getResourceType(), ro);
                registeredAs.add(ro);
            }
            if (operation instanceof ReadMultipleResourcesOperation<?> o) {
                RegisteredOperation<ReadMultipleResourcesOperation<?>> ro
                        = enrichWithMetaInfo(o, READ_MULTIPLE_RESOURCES, ReadMultipleResourcesOperation.class);
                readMultipleResourcesOperations.put(ro.getResourceType(), ro);
                registeredAs.add(ro);
            }
            if (operation instanceof CreateResourceOperation<?> o) {
                RegisteredOperation<CreateResourceOperation<?>> ro
                        = enrichWithMetaInfo(o, CREATE_RESOURCE, CreateResourceOperation.class);
                createResourceOperations.put(ro.getResourceType(), ro);
                registeredAs.add(ro);
            }
            if (operation instanceof UpdateResourceOperation o) {
                RegisteredOperation<UpdateResourceOperation> ro
                        = enrichWithMetaInfo(o, UPDATE_RESOURCE, UpdateResourceOperation.class);
                updateResourceOperations.put(ro.getResourceType(), ro);
                registeredAs.add(ro);
            }
            if (operation instanceof DeleteResourceOperation o) {
                RegisteredOperation<DeleteResourceOperation> ro
                        = enrichWithMetaInfo(o, DELETE_RESOURCE, DeleteResourceOperation.class);
                deleteResourceOperations.put(ro.getResourceType(), ro);
                registeredAs.add(ro);
            }
            if (operation instanceof ReadToOneRelationshipOperation<?, ?> o) {
                RegisteredOperation<ReadToOneRelationshipOperation<?, ?>> ro
                        = enrichWithMetaInfo(o, READ_TO_ONE_RELATIONSHIP, ReadToOneRelationshipOperation.class);
                readToOneRelationshipOperations.computeIfAbsent(ro.getResourceType(), rt -> new HashMap<>())
                        .put(ro.getRelationshipName(), ro);
                registeredAs.add(ro);
            }
            if (operation instanceof ReadToManyRelationshipOperation<?, ?> o) {
                RegisteredOperation<ReadToManyRelationshipOperation<?, ?>> ro
                        = enrichWithMetaInfo(o, READ_TO_MANY_RELATIONSHIP, ReadToManyRelationshipOperation.class);
                readToManyRelationshipOperations.computeIfAbsent(ro.getResourceType(), rt -> new HashMap<>())
                        .put(ro.getRelationshipName(), ro);
                registeredAs.add(ro);
            }
            if (operation instanceof UpdateToOneRelationshipOperation o) {
                RegisteredOperation<UpdateToOneRelationshipOperation> ro
                        = enrichWithMetaInfo(o, UPDATE_TO_ONE_RELATIONSHIP, UpdateToOneRelationshipOperation.class);
                updateToOneRelationshipOperations.computeIfAbsent(ro.getResourceType(), rt -> new HashMap<>())
                        .put(ro.getRelationshipName(), ro);
                registeredAs.add(ro);
            }
            if (operation instanceof UpdateToManyRelationshipOperation o) {
                RegisteredOperation<UpdateToManyRelationshipOperation> ro
                        = enrichWithMetaInfo(o, UPDATE_TO_MANY_RELATIONSHIP, UpdateToManyRelationshipOperation.class);
                updateToManyRelationshipOperations.computeIfAbsent(ro.getResourceType(), rt -> new HashMap<>())
                        .put(ro.getRelationshipName(), ro);
                registeredAs.add(ro);
            }
            if (registeredAs.isEmpty()) {
                log.warn("Failed to register an Operation, unknown operation {}", operation);
                throw new OperationsMisconfigurationException(
                        "Unsupported operation type: %s. The operation must implement one of the supported operations: %s".formatted(
                                operation.getClass().getName(),
                                Stream.of(
                                        ReadResourceByIdOperation.class,
                                        ReadMultipleResourcesOperation.class,
                                        CreateResourceOperation.class,
                                        UpdateResourceOperation.class,
                                        DeleteResourceOperation.class,
                                        ReadToOneRelationshipOperation.class,
                                        ReadToManyRelationshipOperation.class,
                                        UpdateToOneRelationshipOperation.class,
                                        UpdateToManyRelationshipOperation.class
                                ).map(Class::getSimpleName).collect(Collectors.joining(", "))
                        )
                );
            }
            registeredAs.forEach(this::logOperationRegistered);
            registeredAs.forEach(o -> {
                resourceTypesWithAnyOperationConfigured.add(o.getResourceType());
                if (o.getRelationshipName() != null) {
                    relationshipNamesWithAnyOperationConfigured.computeIfAbsent(
                            o.getResourceType(),
                            rt -> new HashSet<>()
                    ).add(o.getRelationshipName());
                }
            });
            return this;
        }

        private void logOperationRegistered(RegisteredOperation<?> registeredOperation) {
            log.info(
                    "{} operation has been registered as {}.",
                    registeredOperation.getOperation().getClass().getSimpleName(),
                    registeredOperation.getRegisteredAs().getSimpleName()
            );
        }

        public OperationsRegistryBuilder operations(ResourceOperation... operations) {
            Validate.notNull(operations);
            Arrays.stream(operations).forEach(this::operation);
            return this;
        }

        public OperationsRegistryBuilder operations(Collection<ResourceOperation> operations) {
            Validate.notNull(operations);
            operations.forEach(this::operation);
            return this;
        }

        public OperationsRegistry build() {
            return new OperationsRegistry(
                    this.readResourceByIdOperations,
                    this.readMultipleResourcesOperations,
                    this.createResourceOperations,
                    this.updateResourceOperations,
                    this.deleteResourceOperations,
                    this.readToOneRelationshipOperations,
                    this.readToManyRelationshipOperations,
                    this.updateToOneRelationshipOperations,
                    this.updateToManyRelationshipOperations,
                    this.resourceTypesWithAnyOperationConfigured,
                    this.relationshipNamesWithAnyOperationConfigured
            );
        }


        private <T extends ResourceOperation> RegisteredOperation<T> enrichWithMetaInfo(T operation,
                                                                                        OperationType operationType,
                                                                                        Class<?> operationClass) {
            Map<String, Object> pluginsInfo = new HashMap<>();
            for (JsonApi4jPlugin plugin : this.plugins) {
                Object pluginInfo = plugin.extractPluginInfoFromOperation(operation, operationClass);
                if (pluginInfo != null) {
                    pluginsInfo.put(plugin.pluginName(), pluginInfo);
                }
            }
            RelationshipName relationshipName = null;
            if (operation instanceof RelationshipOperation ro) {
                relationshipName = ro.relationshipName();
            }
            return RegisteredOperation.<T>builder()
                    .operation(operation)
                    .registeredAs(operationClass)
                    .resourceType(operation.resourceType())
                    .relationshipName(relationshipName)
                    .operationType(operationType)
                    .pluginInfo(Collections.unmodifiableMap(pluginsInfo))
                    .build();
        }

    }

}
