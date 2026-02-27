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
    UPDATE_TO_MANY_RELATIONSHIP("Update/Delete To-Many Relationship", PATCH, TO_MANY_RELATIONSHIP),
    ADD_TO_MANY_RELATIONSHIP("Add To-Many Relationship Linkage", POST, TO_MANY_RELATIONSHIP),
    REMOVE_FROM_MANY_RELATIONSHIP("Remove From To-Many Relationship Linkage", DELETE, TO_MANY_RELATIONSHIP);

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
                                || operationType == OperationType.UPDATE_TO_MANY_RELATIONSHIP
                                || operationType == OperationType.ADD_TO_MANY_RELATIONSHIP
                                || operationType == OperationType.REMOVE_FROM_MANY_RELATIONSHIP)
                .toList();
    }

    public int getHttpStatus() {
        return OperationHttpStatusResolver.resolveSuccessStatus(this);
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

    public enum SubType {
        RESOURCE,
        TO_ONE_RELATIONSHIP,
        TO_MANY_RELATIONSHIP
    }

}
