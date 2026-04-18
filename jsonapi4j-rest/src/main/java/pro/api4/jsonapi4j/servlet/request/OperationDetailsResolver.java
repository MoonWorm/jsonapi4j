package pro.api4.jsonapi4j.servlet.request;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RelationshipDetails;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.RelationshipType;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.exception.MethodNotSupportedException;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationType.Method;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import lombok.Data;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static pro.api4.jsonapi4j.operation.OperationType.Method.DELETE;
import static pro.api4.jsonapi4j.operation.OperationType.Method.GET;
import static pro.api4.jsonapi4j.operation.OperationType.Method.PATCH;
import static pro.api4.jsonapi4j.operation.OperationType.Method.POST;
import static pro.api4.jsonapi4j.operation.OperationType.Method.fromString;

@Slf4j
public class OperationDetailsResolver {

    private final DomainRegistry domainRegistry;

    public OperationDetailsResolver(DomainRegistry domainRegistry) {
        this.domainRegistry = domainRegistry;
    }

    public OperationDetails fromUrlAndMethod(String appRelativePath,
                                             String methodString) {
        log.debug("Resolving operation for path '{}', method '{}'", appRelativePath, methodString);
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
        ResourceType resourceType = domainRegistry.getResourceType(resourceTypeStr);
        if (resourceType == null) {
            throw new OperationNotFoundException(appRelativePath, methodString, "Unknown resource type: " + resourceTypeStr);
        }
        if (pathFragments.size() == 1) {
            if (methodEnum == GET) {
                return resolved(OperationType.READ_MULTIPLE_RESOURCES, resourceType, null);
            } else if (methodEnum == POST) {
                return resolved(OperationType.CREATE_RESOURCE, resourceType, null);
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
                    return resolved(OperationType.READ_RESOURCE_BY_ID, resourceType, null);
                } else if (methodEnum == PATCH) {
                    return resolved(OperationType.UPDATE_RESOURCE, resourceType, null);
                } else if (methodEnum == DELETE) {
                    return resolved(OperationType.DELETE_RESOURCE, resourceType, null);
                } else {
                    throw new MethodNotSupportedException(
                            methodString,
                            Stream.of(GET, PATCH, DELETE).map(Enum::name).collect(Collectors.joining(", "))
                    );
                }
            }
        } else if (pathFragments.size() == 4 && pathFragments.get(2).equals("relationships")) {
            String relationshipStr = pathFragments.get(3);
            RelationshipDetails relationshipDetails = domainRegistry.resolveRelationshipDetails(relationshipStr, resourceType);
            if (relationshipDetails == null) {
                throw new OperationNotFoundException(appRelativePath, methodString, "Unknown relationship: " + relationshipStr);
            }
            RelationshipName relationshipName = relationshipDetails.getRelationshipName();
            RelationshipType relationshipType = relationshipDetails.getRelationshipType();
            if (methodEnum == GET) {
                if (relationshipType == RelationshipType.TO_ONE) {
                    return resolved(OperationType.READ_TO_ONE_RELATIONSHIP, resourceType, relationshipName);
                } else if (relationshipType == RelationshipType.TO_MANY) {
                    return resolved(OperationType.READ_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
                }
            } else if (methodEnum == PATCH) {
                if (relationshipType == RelationshipType.TO_ONE) {
                    return resolved(OperationType.UPDATE_TO_ONE_RELATIONSHIP, resourceType, relationshipName);
                } else if (relationshipType == RelationshipType.TO_MANY) {
                    return resolved(OperationType.UPDATE_TO_MANY_RELATIONSHIP, resourceType, relationshipName);
                }
            } else {
                throw new MethodNotSupportedException(
                        methodString,
                        Stream.of(GET, PATCH).map(Enum::name).collect(Collectors.joining(", "))
                );
            }
        }
        throw new OperationNotFoundException(appRelativePath, methodString);
    }

    private OperationDetails resolved(OperationType operationType, ResourceType resourceType, RelationshipName relationshipName) {
        OperationDetails details = new OperationDetails(operationType, resourceType, relationshipName);
        log.debug("Resolved operation: {}", details);
        return details;
    }

    @Data
    public static class OperationDetails {
        private final OperationType operationType;
        private final ResourceType resourceType;
        private final RelationshipName relationshipName;
    }

}
