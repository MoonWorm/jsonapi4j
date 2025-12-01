package pro.api4.jsonapi4j.operation;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;
import org.apache.commons.lang3.Validate;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.operation.exception.OperationsMisconfigurationException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OperationsRegistry {

    private final Map<ResourceType, ReadResourceByIdOperation<?>> readResourceByIdOperations;
    private final Map<ResourceType, ReadMultipleResourcesOperation<?>> readMultipleResourcesOperations;
    private final Map<ResourceType, CreateResourceOperation<?>> createResourceOperations;
    private final Map<ResourceType, UpdateResourceOperation> updateResourceOperations;
    private final Map<ResourceType, DeleteResourceOperation> deleteResourceOperations;
    private final Map<ResourceType, Map<RelationshipName, ReadToOneRelationshipOperation<?, ?>>> readToOneRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, ReadToManyRelationshipOperation<?, ?>>> readToManyRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, UpdateToOneRelationshipOperation>> updateToOneRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, UpdateToManyRelationshipOperation>> updateToManyRelationshipOperations;
    private final Set<ResourceType> resourceTypesWithAnyOperationConfigured;
    private final Map<ResourceType, Set<RelationshipName>> relationshipNamesWithAnyOperationConfigured;

    private OperationsRegistry(Map<ResourceType, ReadResourceByIdOperation<?>> readResourceByIdOperations,
                               Map<ResourceType, ReadMultipleResourcesOperation<?>> readMultipleResourcesOperations,
                               Map<ResourceType, CreateResourceOperation<?>> createResourceOperations,
                               Map<ResourceType, UpdateResourceOperation> updateResourceOperations,
                               Map<ResourceType, DeleteResourceOperation> deleteResourceOperations,
                               Map<ResourceType, Map<RelationshipName, ReadToOneRelationshipOperation<?, ?>>> readToOneRelationshipOperations,
                               Map<ResourceType, Map<RelationshipName, ReadToManyRelationshipOperation<?, ?>>> readToManyRelationshipOperations,
                               Map<ResourceType, Map<RelationshipName, UpdateToOneRelationshipOperation>> updateToOneRelationshipOperations,
                               Map<ResourceType, Map<RelationshipName, UpdateToManyRelationshipOperation>> updateToManyRelationshipOperations,
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

    public static OperationsRegistryBuilder builder() {
        return new OperationsRegistryBuilder();
    }

    public static OperationsRegistry empty() {
        return builder().build();
    }

    public Set<ResourceType> getResourceTypesWithAnyOperationConfigured() {
        return this.resourceTypesWithAnyOperationConfigured;
    }

    public Set<RelationshipName> getRelationshipNamesWithAnyOperationConfigured(ResourceType resourceType) {
        return SetUtils.emptyIfNull(this.relationshipNamesWithAnyOperationConfigured.get(resourceType));
    }

    public ResourceOperation getResourceOperation(ResourceType resourceType,
                                                  OperationType operationType,
                                                  boolean orElseThrow) {
        if (operationType.getSubType() == OperationType.SubType.RESOURCE) {
            if (operationType == OperationType.READ_RESOURCE_BY_ID) {
                return getReadResourceByIdOperation(resourceType, orElseThrow);
            } else if (operationType == OperationType.READ_MULTIPLE_RESOURCES) {
                return getReadMultipleResourcesOperation(resourceType, orElseThrow);
            } else if (operationType == OperationType.CREATE_RESOURCE) {
                return getCreateResourceOperation(resourceType, orElseThrow);
            } else if (operationType == OperationType.UPDATE_RESOURCE) {
                return getUpdateResourceOperation(resourceType, orElseThrow);
            } else {
                return getDeleteResourceOperation(resourceType, orElseThrow);
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
        if (operationType.getSubType() == OperationType.SubType.TO_ONE_RELATIONSHIP) {
            if (operationType == OperationType.READ_TO_ONE_RELATIONSHIP) {
                return getReadToOneRelationshipOperation(resourceType, relationshipName, orElseThrow);
            } else {
                return getUpdateToOneRelationshipOperation(resourceType, relationshipName, orElseThrow);
            }
        } else if (operationType.getSubType() == OperationType.SubType.TO_MANY_RELATIONSHIP) {
            if (operationType == OperationType.READ_TO_MANY_RELATIONSHIP) {
                return getReadToManyDataRelationshipOperation(resourceType, relationshipName, orElseThrow);
            } else {
                return getUpdateToManyRelationshipOperation(resourceType, relationshipName, orElseThrow);
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
        if (!readResourceByIdOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.READ_RESOURCE_BY_ID, resourceType);
            }
            return null;
        }
        return readResourceByIdOperations.get(resourceType);
    }

    public ReadMultipleResourcesOperation<?> getReadMultipleResourcesOperation(ResourceType resourceType,
                                                                               boolean orElseThrow) {
        if (!readMultipleResourcesOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.READ_MULTIPLE_RESOURCES, resourceType);
            }
            return null;
        }
        return readMultipleResourcesOperations.get(resourceType);
    }

    public CreateResourceOperation<?> getCreateResourceOperation(ResourceType resourceType,
                                                                 boolean orElseThrow) {
        if (!createResourceOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.CREATE_RESOURCE, resourceType);
            }
            return null;
        }
        return createResourceOperations.get(resourceType);
    }

    public UpdateResourceOperation getUpdateResourceOperation(ResourceType resourceType,
                                                              boolean orElseThrow) {
        if (!updateResourceOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.UPDATE_RESOURCE, resourceType);
            }
            return null;
        }
        return updateResourceOperations.get(resourceType);
    }

    public DeleteResourceOperation getDeleteResourceOperation(ResourceType resourceType,
                                                              boolean orElseThrow) {
        if (!deleteResourceOperations.containsKey(resourceType)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.DELETE_RESOURCE, resourceType);
            }
            return null;
        }
        return deleteResourceOperations.get(resourceType);
    }

    public ReadToOneRelationshipOperation<?, ?> getReadToOneRelationshipOperation(ResourceType resourceType,
                                                                                  RelationshipName relationshipName,
                                                                                  boolean orElseThrow) {
        if (!readToOneRelationshipOperations.containsKey(resourceType)
                || !readToOneRelationshipOperations.get(resourceType).containsKey(relationshipName)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.READ_TO_ONE_RELATIONSHIP, resourceType, relationshipName);
            }
            return null;
        }
        return readToOneRelationshipOperations.get(resourceType).get(relationshipName);
    }

    public ReadToManyRelationshipOperation<?, ?> getReadToManyDataRelationshipOperation(ResourceType resourceType,
                                                                                        RelationshipName relationshipName,
                                                                                        boolean orElseThrow) {
        if (!readToManyRelationshipOperations.containsKey(resourceType)
                || !readToManyRelationshipOperations.get(resourceType).containsKey(relationshipName)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.READ_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
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
        return MapUtils.emptyIfNull(updateToOneRelationshipOperations.get(resourceType))
                .values()
                .stream()
                .toList();
    }

    public UpdateToOneRelationshipOperation getUpdateToOneRelationshipOperation(ResourceType resourceType,
                                                                                RelationshipName relationshipName,
                                                                                boolean orElseThrow) {
        if (!updateToOneRelationshipOperations.containsKey(resourceType)
                || !updateToOneRelationshipOperations.get(resourceType).containsKey(relationshipName)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.UPDATE_TO_ONE_RELATIONSHIP, resourceType, relationshipName);
            }
            return null;
        }
        return updateToOneRelationshipOperations.get(resourceType).get(relationshipName);
    }

    public UpdateToManyRelationshipOperation getUpdateToManyRelationshipOperation(ResourceType resourceType,
                                                                                  RelationshipName relationshipName,
                                                                                  boolean orElseThrow) {
        if (!updateToManyRelationshipOperations.containsKey(resourceType)
                || !updateToManyRelationshipOperations.get(resourceType).containsKey(relationshipName)) {
            if (orElseThrow) {
                throw new OperationNotFoundException(OperationType.UPDATE_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
            }
            return null;
        }
        return updateToManyRelationshipOperations.get(resourceType).get(relationshipName);
    }

    public List<UpdateToManyRelationshipOperation> getUpdateToManyRelationshipOperationsFor(ResourceType resourceType) {
        return MapUtils.emptyIfNull(updateToManyRelationshipOperations.get(resourceType))
                .values()
                .stream()
                .toList();
    }

    public List<? extends Operation> getAllOperations() {
        List<Operation> result = new ArrayList<>();
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

    @Slf4j
    public static class OperationsRegistryBuilder {

        private final Map<ResourceType, ReadResourceByIdOperation<?>> readResourceByIdOperations;
        private final Map<ResourceType, ReadMultipleResourcesOperation<?>> readMultipleResourcesOperations;
        private final Map<ResourceType, CreateResourceOperation<?>> createResourceOperations;
        private final Map<ResourceType, UpdateResourceOperation> updateResourceOperations;
        private final Map<ResourceType, DeleteResourceOperation> deleteResourceOperations;

        private final Map<ResourceType, Map<RelationshipName, ReadToOneRelationshipOperation<?, ?>>> readToOneRelationshipOperations;
        private final Map<ResourceType, Map<RelationshipName, ReadToManyRelationshipOperation<?, ?>>> readToManyRelationshipOperations;
        private final Map<ResourceType, Map<RelationshipName, UpdateToOneRelationshipOperation>> updateToOneRelationshipOperations;
        private final Map<ResourceType, Map<RelationshipName, UpdateToManyRelationshipOperation>> updateToManyRelationshipOperations;

        private final Set<ResourceType> resourceTypesWithAnyOperationConfigured;
        private final Map<ResourceType, Set<RelationshipName>> relationshipNamesWithAnyOperationConfigured;

        private OperationsRegistryBuilder() {
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

        public OperationsRegistryBuilder operation(ResourceOperation operation) {
            Validate.notNull(operation);
            boolean isRegistered = false;
            if (operation instanceof ReadResourceByIdOperation<?> o) {
                this.readResourceByIdOperations.put(o.resourceType(), o);
                isRegistered = true;
                logOperationRegistered(o, ReadResourceByIdOperation.class);
            }
            if (operation instanceof ReadMultipleResourcesOperation<?> o) {
                this.readMultipleResourcesOperations.put(o.resourceType(), o);
                isRegistered = true;
                logOperationRegistered(o, ReadMultipleResourcesOperation.class);
            }
            if (operation instanceof CreateResourceOperation<?> o) {
                this.createResourceOperations.put(o.resourceType(), o);
                isRegistered = true;
                logOperationRegistered(o, CreateResourceOperation.class);
            }
            if (operation instanceof UpdateResourceOperation o) {
                this.updateResourceOperations.put(o.resourceType(), o);
                isRegistered = true;
                logOperationRegistered(o, UpdateResourceOperation.class);
            }
            if (operation instanceof DeleteResourceOperation o) {
                this.deleteResourceOperations.put(o.resourceType(), o);
                isRegistered = true;
                logOperationRegistered(o, DeleteResourceOperation.class);
            }
            if (operation instanceof ReadToOneRelationshipOperation<?, ?> o) {
                this.readToOneRelationshipOperations.computeIfAbsent(
                        o.resourceType(),
                        rt -> new HashMap<>()
                ).put(o.relationshipName(), o);
                isRegistered = true;
                logOperationRegistered(o, ReadToOneRelationshipOperation.class);
            }
            if (operation instanceof ReadToManyRelationshipOperation<?, ?> o) {
                this.readToManyRelationshipOperations.computeIfAbsent(
                        o.resourceType(),
                        rt -> new HashMap<>()
                ).put(o.relationshipName(), o);
                isRegistered = true;
                logOperationRegistered(o, ReadToManyRelationshipOperation.class);
            }
            if (operation instanceof UpdateToOneRelationshipOperation o) {
                this.updateToOneRelationshipOperations.computeIfAbsent(
                        o.resourceType(),
                        rt -> new HashMap<>()
                ).put(o.relationshipName(), o);
                isRegistered = true;
                logOperationRegistered(o, UpdateToOneRelationshipOperation.class);
            }
            if (operation instanceof UpdateToManyRelationshipOperation o) {
                this.updateToManyRelationshipOperations.computeIfAbsent(
                        o.resourceType(),
                        rt -> new HashMap<>()
                ).put(o.relationshipName(), o);
                isRegistered = true;
                logOperationRegistered(o, UpdateToManyRelationshipOperation.class);
            }
            if (isRegistered) {
                resourceTypesWithAnyOperationConfigured.add(operation.resourceType());
                if (operation instanceof RelationshipOperation relOp) {
                    relationshipNamesWithAnyOperationConfigured.computeIfAbsent(
                            relOp.resourceType(), k -> new HashSet<>()
                    ).add(relOp.relationshipName());
                }
            } else {
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

            return this;
        }

        private void logOperationRegistered(ResourceOperation operationType,
                                            Class<?> registeredAsType) {
            log.info("{} operation has been registered as {}.", operationType.getClass().getSimpleName(), registeredAsType.getSimpleName());
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
                    Collections.unmodifiableMap(this.readResourceByIdOperations),
                    Collections.unmodifiableMap(this.readMultipleResourcesOperations),
                    Collections.unmodifiableMap(this.createResourceOperations),
                    Collections.unmodifiableMap(this.updateResourceOperations),
                    Collections.unmodifiableMap(this.deleteResourceOperations),

                    Collections.unmodifiableMap(this.readToOneRelationshipOperations),
                    Collections.unmodifiableMap(this.readToManyRelationshipOperations),
                    Collections.unmodifiableMap(this.updateToOneRelationshipOperations),
                    Collections.unmodifiableMap(this.updateToManyRelationshipOperations),

                    Collections.unmodifiableSet(this.resourceTypesWithAnyOperationConfigured),
                    Collections.unmodifiableMap(this.relationshipNamesWithAnyOperationConfigured)
            );
        }
    }

}
