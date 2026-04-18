package pro.api4.jsonapi4j.operation;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;
import java.util.List;

import static pro.api4.jsonapi4j.operation.OperationType.Method.*;
import static pro.api4.jsonapi4j.operation.OperationType.SubType.*;

@Getter
@AllArgsConstructor
public enum OperationType {
    READ_RESOURCE_BY_ID("Read resource by ID", GET, RESOURCE),
    READ_MULTIPLE_RESOURCES("Read multiple resources", GET, RESOURCE),
    CREATE_RESOURCE("Create resource", POST, RESOURCE),
    UPDATE_RESOURCE("Update resource", PATCH, RESOURCE),
    DELETE_RESOURCE("Delete resource", DELETE, RESOURCE),
    READ_TO_ONE_RELATIONSHIP("Read To-One Relationship", GET, TO_ONE_RELATIONSHIP),
    UPDATE_TO_ONE_RELATIONSHIP("Update/Delete To-One Relationship", PATCH, TO_ONE_RELATIONSHIP),
    READ_TO_MANY_RELATIONSHIP("Read To-Many Relationship", GET, TO_MANY_RELATIONSHIP),
    UPDATE_TO_MANY_RELATIONSHIPS("Update/Delete To-Many Relationships", PATCH, TO_MANY_RELATIONSHIP),
    ADD_TO_MANY_RELATIONSHIP("Add To-Many Relationship Members", POST, TO_MANY_RELATIONSHIP),
    DELETE_TO_MANY_RELATIONSHIP("Delete To-Many Relationship Members", DELETE, TO_MANY_RELATIONSHIP);

    private final String name;
    private final Method method;
    private final SubType subType;

    public static List<OperationType> getResourceOperationTypes() {
        return Arrays.stream(values())
                .filter(ot -> ot.getSubType() == RESOURCE)
                .toList();
    }

    public static List<OperationType> getToOneRelationshipOperationTypes() {
        return Arrays.stream(values())
                .filter(ot -> ot.getSubType() == TO_ONE_RELATIONSHIP)
                .toList();
    }

    public static List<OperationType> getToManyRelationshipOperationTypes() {
        return Arrays.stream(values())
                .filter(ot -> ot.getSubType() == TO_MANY_RELATIONSHIP)
                .toList();
    }

    public static List<OperationType> getAllRelationshipOperationTypes() {
        return Arrays.stream(values())
                .filter(ot -> ot.getSubType() == TO_ONE_RELATIONSHIP
                        || ot.getSubType() == TO_MANY_RELATIONSHIP)
                .toList();
    }

    // ones that has {resourceId} in the URL
    public static List<OperationType> getExistingResourceAwareOperations() {
        return Arrays.stream(values())
                .filter(operationType ->
                        operationType == OperationType.READ_RESOURCE_BY_ID
                                || operationType == OperationType.UPDATE_RESOURCE
                                || operationType == OperationType.DELETE_RESOURCE
                                || operationType == OperationType.READ_TO_ONE_RELATIONSHIP
                                || operationType == OperationType.UPDATE_TO_ONE_RELATIONSHIP
                                || operationType == OperationType.READ_TO_MANY_RELATIONSHIP
                                || operationType == OperationType.UPDATE_TO_MANY_RELATIONSHIPS
                                || operationType == OperationType.ADD_TO_MANY_RELATIONSHIP
                                || operationType == OperationType.DELETE_TO_MANY_RELATIONSHIP)
                .toList();
    }

    public int getHttpStatus() {
        return switch (this) {
            case READ_RESOURCE_BY_ID, READ_MULTIPLE_RESOURCES,
                 READ_TO_ONE_RELATIONSHIP, READ_TO_MANY_RELATIONSHIP -> 200;
            case CREATE_RESOURCE -> 201;
            case UPDATE_RESOURCE, DELETE_RESOURCE,
                 UPDATE_TO_ONE_RELATIONSHIP, UPDATE_TO_MANY_RELATIONSHIPS -> 202;
            case ADD_TO_MANY_RELATIONSHIP, DELETE_TO_MANY_RELATIONSHIP -> 204;
        };
    }

    public enum Method {
        GET, POST, PATCH, DELETE;

        public static Method fromString(String method) {
            return valueOf(method.toUpperCase());
        }

        public static boolean isSupportedMethod(String method) {
            try {
                Method.fromString(method);
                return true;
            } catch (IllegalArgumentException e) {
                return false;
            }
        }
    }

    /**
     * Formats a human-readable URL pattern for this operation type,
     * e.g. {@code GET /users/{id}} or {@code PATCH /users/{id}/relationships/placeOfBirth}.
     *
     * @param resourceType     the resource type string (e.g. "users")
     * @param relationshipName the relationship name, or {@code null} for resource operations
     * @return formatted URL pattern prefixed with the HTTP method
     */
    public String formatUrl(String resourceType, String relationshipName) {
        return switch (this) {
            case READ_MULTIPLE_RESOURCES, CREATE_RESOURCE ->
                    String.format("%s /%s (%s)", method, resourceType, name);
            case READ_RESOURCE_BY_ID, UPDATE_RESOURCE, DELETE_RESOURCE ->
                    String.format("%s /%s/{id} (%s)", method, resourceType, name);
            case READ_TO_ONE_RELATIONSHIP, UPDATE_TO_ONE_RELATIONSHIP,
                 READ_TO_MANY_RELATIONSHIP, UPDATE_TO_MANY_RELATIONSHIPS,
                 ADD_TO_MANY_RELATIONSHIP, DELETE_TO_MANY_RELATIONSHIP ->
                    String.format("%s /%s/{id}/relationships/%s (%s)", method, resourceType, relationshipName, name);
        };
    }

    public enum SubType {
        RESOURCE,
        TO_ONE_RELATIONSHIP,
        TO_MANY_RELATIONSHIP
    }

}
