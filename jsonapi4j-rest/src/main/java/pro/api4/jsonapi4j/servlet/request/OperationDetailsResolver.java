package pro.api4.jsonapi4j.servlet.request;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.RegisteredRelationship;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.exception.MethodNotSupportedException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationType.Method;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.exception.BadJsonApiRequestException;
import lombok.Data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pro.api4.jsonapi4j.operation.OperationType.Method.DELETE;
import static pro.api4.jsonapi4j.operation.OperationType.Method.GET;
import static pro.api4.jsonapi4j.operation.OperationType.Method.PATCH;
import static pro.api4.jsonapi4j.operation.OperationType.Method.POST;
import static pro.api4.jsonapi4j.operation.OperationType.Method.fromString;

public class OperationDetailsResolver {

    private final DomainRegistry domainRegistry;

    public OperationDetailsResolver(DomainRegistry domainRegistry) {
        this.domainRegistry = domainRegistry;
    }

    public OperationDetails fromUrlAndMethod(String appRelativePath,
                                             String methodString) {
        String path = appRelativePath;
        if (path.startsWith("/")) {
            path = path.substring(1);
        }
        List<String> pathFragments = Arrays.asList(path.split("/"));
        if (pathFragments.isEmpty()) {
            throw new OperationNotFoundException(appRelativePath, methodString, "Invalid JSON:API path");
        }
        Method methodEnum = fromString(methodString);

        String resourceTypeStr = pathFragments.getFirst();
        ResourceType resourceType = resolveResourceType(resourceTypeStr);
        if (resourceType == null) {
            throw new OperationNotFoundException(appRelativePath, methodString, "Unknown resource type: " + resourceTypeStr);
        }

        if (pathFragments.size() == 1) {
            if (methodEnum == GET) {
                return new OperationDetails(OperationType.READ_MULTIPLE_RESOURCES, resourceType, null);
            } else if (methodEnum == POST) {
                return new OperationDetails(OperationType.CREATE_RESOURCE, resourceType, null);
            } else {
                throw new MethodNotSupportedException(
                        methodString,
                        Stream.of(GET, POST).map(Enum::name).collect(Collectors.joining(", "))
                );
            }
        } else if (pathFragments.size() == 2) {
            String secondFragment = pathFragments.get(1);
            if (!secondFragment.isBlank()) {
                if (methodEnum == GET) {
                    return new OperationDetails(OperationType.READ_RESOURCE_BY_ID, resourceType, null);
                } else if (methodEnum == PATCH) {
                    return new OperationDetails(OperationType.UPDATE_RESOURCE, resourceType, null);
                } else if (methodEnum == DELETE) {
                    return new OperationDetails(OperationType.DELETE_RESOURCE, resourceType, null);
                } else {
                    throw new MethodNotSupportedException(
                            methodString,
                            Stream.of(GET, PATCH, DELETE).map(Enum::name).collect(Collectors.joining(", "))
                    );
                }
            }
        } else if (pathFragments.size() == 4 && pathFragments.get(2).equals("relationships")) {

            String relationshipStr = pathFragments.get(3);
            RelationshipDetails relationshipDetails = resolveRelationshipDetails(relationshipStr, resourceType);
            if (relationshipDetails == null || relationshipDetails.getRelationshipName() == null || relationshipDetails.getSubType() == null) {
                throw new OperationNotFoundException(appRelativePath, methodString, "Unknown relationship: " + relationshipStr);
            }
            RelationshipName relationshipName = relationshipDetails.getRelationshipName();
            OperationType.SubType relationshipSubType = relationshipDetails.getSubType();

            if (methodEnum == GET) {
                if (relationshipSubType == OperationType.SubType.TO_ONE_RELATIONSHIP) {
                    return new OperationDetails(OperationType.READ_TO_ONE_RELATIONSHIP, resourceType, relationshipName);
                } else if (relationshipSubType == OperationType.SubType.TO_MANY_RELATIONSHIP) {
                    return new OperationDetails(OperationType.READ_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
                }
            } else if (methodEnum == PATCH) {
                if (relationshipSubType == OperationType.SubType.TO_ONE_RELATIONSHIP) {
                    return new OperationDetails(OperationType.UPDATE_TO_ONE_RELATIONSHIP, resourceType, relationshipName);
                } else if (relationshipSubType == OperationType.SubType.TO_MANY_RELATIONSHIP) {
                    return new OperationDetails(OperationType.UPDATE_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
                }
            } else if (relationshipSubType == OperationType.SubType.TO_MANY_RELATIONSHIP && methodEnum == POST) {
                return new OperationDetails(OperationType.ADD_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
            } else if (relationshipSubType == OperationType.SubType.TO_MANY_RELATIONSHIP && methodEnum == DELETE) {
                return new OperationDetails(OperationType.REMOVE_FROM_MANY_RELATIONSHIP, resourceType, relationshipName);
            } else if (relationshipSubType == OperationType.SubType.TO_ONE_RELATIONSHIP) {
                throw new MethodNotSupportedException(
                        methodString,
                        Stream.of(GET, PATCH).map(Enum::name).collect(Collectors.joining(", "))
                );
            } else if (relationshipSubType == OperationType.SubType.TO_MANY_RELATIONSHIP) {
                throw new MethodNotSupportedException(
                        methodString,
                        Stream.of(GET, PATCH, POST, DELETE).map(Enum::name).collect(Collectors.joining(", "))
                );
            }
        }
        throw new OperationNotFoundException(appRelativePath, methodString);
    }

    public void validateIncludes(Set<String> originalIncludes,
                                 OperationDetails operationDetails,
                                 JsonApi4jCompatibilityMode compatibilityMode) {
        if (compatibilityMode != JsonApi4jCompatibilityMode.STRICT
                || operationDetails == null
                || operationDetails.getOperationType() == null
                || operationDetails.getOperationType().getMethod() != GET
                || originalIncludes == null
                || originalIncludes.isEmpty()) {
            return;
        }
        for (String include : originalIncludes) {
            validateSingleIncludePath(include, operationDetails);
        }
    }

    private ResourceType resolveResourceType(String resourceTypeStr) {
        return resourceTypeFromStr(resourceTypeStr);
    }

    private ResourceType resourceTypeFromStr(String resourceTypeStr) {
        for (ResourceType resourceType : domainRegistry.getResourceTypes()) {
            if (resourceType.getType().equals(resourceTypeStr)) {
                return resourceType;
            }
        }
        return null;
    }

    private RelationshipDetails resolveRelationshipDetails(String relationshipStr, ResourceType resourceType) {
        for (RelationshipName relationship : domainRegistry.getToManyRelationshipNames(resourceType)) {
            if (relationship.getName().equals(relationshipStr)) {
                return new RelationshipDetails(relationship, OperationType.SubType.TO_MANY_RELATIONSHIP);
            }
        }
        for (RelationshipName relationship : domainRegistry.getToOneRelationshipNames(resourceType)) {
            if (relationship.getName().equals(relationshipStr)) {
                return new RelationshipDetails(relationship, OperationType.SubType.TO_ONE_RELATIONSHIP);
            }
        }
        return null;
    }

    private void validateSingleIncludePath(String includePath,
                                           OperationDetails operationDetails) {
        if (StringUtils.isBlank(includePath)) {
            throw invalidIncludePath(includePath, "Include path must not be blank");
        }
        List<String> pathSegments = Arrays.stream(includePath.split("\\.", -1))
                .map(String::trim)
                .toList();
        if (pathSegments.isEmpty() || pathSegments.stream().anyMatch(StringUtils::isBlank)) {
            throw invalidIncludePath(includePath, "Include path contains an empty relationship segment");
        }

        RelationshipName targetRelationshipName = operationDetails.getRelationshipName();
        if (targetRelationshipName != null && !targetRelationshipName.getName().equals(pathSegments.getFirst())) {
            throw invalidIncludePath(
                    includePath,
                    "Relationship endpoint includes must start with '%s'".formatted(targetRelationshipName.getName())
            );
        }

        Set<ResourceType> candidateResourceTypes = new HashSet<>();
        candidateResourceTypes.add(operationDetails.getResourceType());

        for (String relationshipNameSegment : pathSegments) {
            Set<ResourceType> nextResourceTypes = new HashSet<>();
            boolean relationshipFound = false;

            for (ResourceType candidateResourceType : candidateResourceTypes) {
                List<RegisteredRelationship<? extends Relationship<?>>> matchingRelationships
                        = resolveRelationshipsByName(candidateResourceType, relationshipNameSegment);
                if (!matchingRelationships.isEmpty()) {
                    relationshipFound = true;
                    matchingRelationships.forEach(relationship ->
                            nextResourceTypes.addAll(resolveNextResourceTypes(relationship))
                    );
                }
            }

            if (!relationshipFound) {
                throw invalidIncludePath(
                        includePath,
                        "Unknown include path segment '%s'".formatted(relationshipNameSegment)
                );
            }

            candidateResourceTypes = nextResourceTypes;
        }
    }

    private List<RegisteredRelationship<? extends Relationship<?>>> resolveRelationshipsByName(
            ResourceType resourceType,
            String relationshipNameSegment
    ) {
        List<RegisteredRelationship<? extends Relationship<?>>> result = new ArrayList<>();
        domainRegistry.getToManyRelationships(resourceType).forEach(relationship -> {
            if (relationship.getRelationshipName().getName().equals(relationshipNameSegment)) {
                result.add(relationship);
            }
        });
        domainRegistry.getToOneRelationships(resourceType).forEach(relationship -> {
            if (relationship.getRelationshipName().getName().equals(relationshipNameSegment)) {
                result.add(relationship);
            }
        });
        return result;
    }

    private Set<ResourceType> resolveNextResourceTypes(RegisteredRelationship<? extends Relationship<?>> relationship) {
        try {
            @SuppressWarnings("rawtypes")
            Relationship rawRelationship = relationship.getRelationship();
            Object resolvedType = rawRelationship.resolveResourceIdentifierType(null);
            if (resolvedType instanceof String resolvedTypeString && StringUtils.isNotBlank(resolvedTypeString)) {
                ResourceType resourceType = resolveResourceType(resolvedTypeString);
                if (resourceType != null) {
                    return Set.of(resourceType);
                }
            }
        } catch (RuntimeException ignored) {
            // no-op: some relationship implementations may need non-null DTO to resolve type
        }
        return domainRegistry.getResourceTypes();
    }

    private BadJsonApiRequestException invalidIncludePath(String includePath,
                                                          String detail) {
        return new BadJsonApiRequestException(
                DefaultErrorCodes.VALUE_INVALID_FORMAT,
                IncludeAwareRequest.INCLUDE_PARAM,
                "Invalid include path '%s'. %s".formatted(includePath, detail)
        );
    }

    @Data
    private static final class RelationshipDetails {
        private final RelationshipName relationshipName;
        private final OperationType.SubType subType;
    }

    @Data
    public static class OperationDetails {
        private final OperationType operationType;
        private final ResourceType resourceType;
        private final RelationshipName relationshipName;
    }

}
