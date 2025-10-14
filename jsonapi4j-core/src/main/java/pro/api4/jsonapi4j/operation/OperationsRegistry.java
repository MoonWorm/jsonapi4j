package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.operation.exception.OperationsMisconfigurationException;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.collections4.SetUtils;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

public class OperationsRegistry {

    public static final OperationsRegistry EMPTY = new OperationsRegistry(
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet(),
            Collections.emptySet()
    );

    private final Map<ResourceType, ReadResourceByIdOperation<?>> readResourceByIdOperations;
    private final Map<ResourceType, ReadMultipleResourcesOperation<?>> readMultipleResourcesOperations;
    private final Map<ResourceType, CreateResourceOperation<?>> createResourceOperations;
    private final Map<ResourceType, UpdateResourceOperation> updateResourceOperations;
    private final Map<ResourceType, DeleteResourceOperation> deleteResourceOperations;

    private final Map<ResourceType, Map<RelationshipName, ReadToOneRelationshipOperation<?, ?>>> readToOneRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, ReadToManyRelationshipOperation<?, ?>>> readToManyRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, UpdateToOneRelationshipOperation>> updateToOneRelationshipOperations;
    private final Map<ResourceType, Map<RelationshipName, UpdateToManyRelationshipOperation>> updateToManyRelationshipOperations;

    private final Set<ResourceType> resourceTypesWithAnyOperationConfigured = new HashSet<>();
    private final Map<ResourceType, Set<RelationshipName>> relationshipNamesWithAnyOperationConfigured = new HashMap<>();

    public OperationsRegistry(
            Set<ReadResourceByIdOperation<?>> readResourceByIdOperations,
            Set<ReadMultipleResourcesOperation<?>> readMultipleResourcesOperations,

            Set<CreateResourceOperation<?>> createResourceOperations,
            Set<UpdateResourceOperation> updateResourceOperations,
            Set<DeleteResourceOperation> deleteResourceOperations,

            Set<ReadToOneRelationshipOperation<?, ?>> readToOneRelationshipOperations,
            Set<ReadToManyRelationshipOperation<?, ?>> readToManyRelationshipOperations,
            Set<UpdateToOneRelationshipOperation> updateToOneRelationshipOperations,
            Set<UpdateToManyRelationshipOperation> updateToManyRelationshipOperations
    ) {
        // group resource operations
        this.readResourceByIdOperations = groupResourceOperation(readResourceByIdOperations);
        this.readMultipleResourcesOperations = groupResourceOperation(readMultipleResourcesOperations);
        this.createResourceOperations = groupResourceOperation(createResourceOperations);
        this.updateResourceOperations = groupResourceOperation(updateResourceOperations);
        this.deleteResourceOperations = groupResourceOperation(deleteResourceOperations);

        // group relationship operations
        this.readToOneRelationshipOperations = groupRelationshipOperation(readToOneRelationshipOperations);
        this.readToManyRelationshipOperations = groupRelationshipOperation(readToManyRelationshipOperations);
        this.updateToOneRelationshipOperations = groupRelationshipOperation(updateToOneRelationshipOperations);
        this.updateToManyRelationshipOperations = groupRelationshipOperation(updateToManyRelationshipOperations);
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

    public boolean isAnyToManyRelationshipOperationsConfigured(ResourceType resourceType,
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

    private <T extends ResourceOperation> Map<ResourceType, T> groupResourceOperation(Set<T> operations) {
        return operations.stream()
                .peek(o -> resourceTypesWithAnyOperationConfigured.add(o.resourceType()))
                .collect(Collectors.toMap(
                        ResourceOperation::resourceType,
                        Function.identity(),
                        (first, second) -> {
                            throw new OperationsMisconfigurationException(
                                    "Two conflicting resource operations: "
                                            + first.getClass().getSimpleName()
                                            + " and "
                                            + second.getClass().getSimpleName()
                                            + ". The both implement the same operation for "
                                            + first.resourceType().getType()
                                            + " resource type"
                            );
                        }
                ));
    }

    private <T extends RelationshipOperation> Map<ResourceType, Map<RelationshipName, T>> groupRelationshipOperation(Set<T> operations) {
        return operations.stream()
                .peek(o -> {
                    resourceTypesWithAnyOperationConfigured.add(o.parentResourceType());
                    relationshipNamesWithAnyOperationConfigured.computeIfAbsent(o.parentResourceType(), k -> new HashSet<>())
                            .add(o.relationshipName());
                })
                .collect(Collectors.groupingBy(
                        RelationshipOperation::parentResourceType,
                        Collectors.toMap(
                                RelationshipOperation::relationshipName,
                                Function.identity(),
                                (first, second) -> {
                                    throw new OperationsMisconfigurationException(
                                            "Two conflicting relationship operations: "
                                                    + first.getClass().getSimpleName()
                                                    + " and "
                                                    + second.getClass().getSimpleName()
                                                    + ". The both implement the same operation for "
                                                    + first.parentResourceType().getType()
                                                    + " resource type"
                                                    + "and for "
                                                    + first.relationshipName().getName()
                                                    + " relationship"
                                    );
                                }
                        )
                ));
    }

}
