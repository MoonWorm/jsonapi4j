package pro.api4.jsonapi4j.operation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import pro.api4.jsonapi4j.http.HttpStatusCodes;

import java.util.Arrays;
import java.util.List;

import static pro.api4.jsonapi4j.operation.OperationType.Method.*;
import static pro.api4.jsonapi4j.operation.OperationType.SubType.*;

/**
 * Enumerates every JSON:API operation type supported by the framework.
 * <p>
 * Each constant carries a human-readable {@link #getName()}, the HTTP {@link Method},
 * and a {@link SubType} that categorises the operation as a resource or relationship operation.
 * <p>
 * Used throughout the framework for request routing, HTTP status code resolution,
 * validation dispatch, and plugin visitor invocation.
 */
@Getter
@AllArgsConstructor
public enum OperationType {
    /** {@code GET /{resourceType}/{id}} — fetch a single resource by its identifier. */
    READ_RESOURCE_BY_ID("Read resource by ID", GET, RESOURCE),
    /** {@code GET /{resourceType}} — fetch a (possibly paginated/filtered) list of resources. */
    READ_MULTIPLE_RESOURCES("Read multiple resources", GET, RESOURCE),
    /** {@code POST /{resourceType}} — create a new resource. */
    CREATE_RESOURCE("Create resource", POST, RESOURCE),
    /** {@code PATCH /{resourceType}/{id}} — update an existing resource. */
    UPDATE_RESOURCE("Update resource", PATCH, RESOURCE),
    /** {@code DELETE /{resourceType}/{id}} — delete an existing resource. */
    DELETE_RESOURCE("Delete resource", DELETE, RESOURCE),
    /** {@code GET /{resourceType}/{id}/relationships/{name}} — fetch a to-one relationship object. */
    READ_TO_ONE_RELATIONSHIP("Read To-One Relationship", GET, TO_ONE_RELATIONSHIP),
    /** {@code PATCH /{resourceType}/{id}/relationships/{name}} — update or clear a to-one relationship. */
    UPDATE_TO_ONE_RELATIONSHIP("Update/Delete To-One Relationship", PATCH, TO_ONE_RELATIONSHIP),
    /** {@code GET /{resourceType}/{id}/relationships/{name}} — fetch a to-many relationship object. */
    READ_TO_MANY_RELATIONSHIP("Read To-Many Relationship", GET, TO_MANY_RELATIONSHIP),
    /** {@code PATCH /{resourceType}/{id}/relationships/{name}} — replace the full to-many relationship member set. */
    UPDATE_TO_MANY_RELATIONSHIPS("Update/Delete To-Many Relationships", PATCH, TO_MANY_RELATIONSHIP),
    /** {@code POST /{resourceType}/{id}/relationships/{name}} — add members to a to-many relationship. */
    ADD_TO_MANY_RELATIONSHIP("Add To-Many Relationship Members", POST, TO_MANY_RELATIONSHIP),
    /** {@code DELETE /{resourceType}/{id}/relationships/{name}} — remove members from a to-many relationship. */
    DELETE_TO_MANY_RELATIONSHIP("Delete To-Many Relationship Members", DELETE, TO_MANY_RELATIONSHIP);

    private final String name;
    private final Method method;
    private final SubType subType;

    /**
     * Returns all resource-level operation types (CRUD on primary resources).
     *
     * @return unmodifiable list of resource operation types
     */
    public static List<OperationType> getResourceOperationTypes() {
        return Arrays.stream(values())
                .filter(ot -> ot.getSubType() == RESOURCE)
                .toList();
    }

    /**
     * Returns all to-one relationship operation types.
     *
     * @return unmodifiable list of to-one relationship operation types
     */
    public static List<OperationType> getToOneRelationshipOperationTypes() {
        return Arrays.stream(values())
                .filter(ot -> ot.getSubType() == TO_ONE_RELATIONSHIP)
                .toList();
    }

    /**
     * Returns all to-many relationship operation types.
     *
     * @return unmodifiable list of to-many relationship operation types
     */
    public static List<OperationType> getToManyRelationshipOperationTypes() {
        return Arrays.stream(values())
                .filter(ot -> ot.getSubType() == TO_MANY_RELATIONSHIP)
                .toList();
    }

    /**
     * Returns all relationship operation types (both to-one and to-many).
     *
     * @return unmodifiable list of all relationship operation types
     */
    public static List<OperationType> getAllRelationshipOperationTypes() {
        return Arrays.stream(values())
                .filter(ot -> ot.getSubType() == TO_ONE_RELATIONSHIP
                        || ot.getSubType() == TO_MANY_RELATIONSHIP)
                .toList();
    }

    /**
     * Returns all operation types whose URL includes a {@code {resourceId}} path segment,
     * i.e. every operation except {@link #READ_MULTIPLE_RESOURCES} and {@link #CREATE_RESOURCE}.
     *
     * @return unmodifiable list of resource-id-aware operation types
     */
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

    /**
     * Returns the HTTP success status code for this operation type, as defined by the JSON:API specification.
     *
     * @return HTTP status code (e.g. 200, 201, 204)
     */
    public int getHttpStatus() {
        return switch (this) {
            case READ_RESOURCE_BY_ID, READ_MULTIPLE_RESOURCES,
                 READ_TO_ONE_RELATIONSHIP, READ_TO_MANY_RELATIONSHIP -> HttpStatusCodes.SC_200_OK.getCode();
            case CREATE_RESOURCE -> HttpStatusCodes.SC_201_CREATED.getCode();
            case UPDATE_RESOURCE, DELETE_RESOURCE,
                 UPDATE_TO_ONE_RELATIONSHIP, UPDATE_TO_MANY_RELATIONSHIPS,
                 ADD_TO_MANY_RELATIONSHIP, DELETE_TO_MANY_RELATIONSHIP -> HttpStatusCodes.SC_204_NO_CONTENT.getCode();
        };
    }

    /** HTTP methods used by JSON:API operations. */
    public enum Method {
        GET, POST, PATCH, DELETE;

        /**
         * Parses an HTTP method string (case-insensitive) into a {@link Method} constant.
         *
         * @param method the HTTP method string (e.g. {@code "GET"}, {@code "patch"})
         * @return the corresponding {@link Method}
         * @throws IllegalArgumentException if the string does not match a supported method
         */
        public static Method fromString(String method) {
            return valueOf(method.toUpperCase());
        }

        /**
         * Returns {@code true} if the given string is a supported HTTP method.
         *
         * @param method the HTTP method string to check
         * @return {@code true} if supported, {@code false} otherwise
         */
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

    /** Categorises an operation as acting on a primary resource or on a relationship. */
    public enum SubType {
        /** The operation targets a primary resource (CRUD). */
        RESOURCE,
        /** The operation targets a to-one relationship. */
        TO_ONE_RELATIONSHIP,
        /** The operation targets a to-many relationship. */
        TO_MANY_RELATIONSHIP
    }

}
