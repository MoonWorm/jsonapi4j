package pro.api4.jsonapi4j.operation;

import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;

public final class OperationHttpStatusResolver {

    private OperationHttpStatusResolver() {

    }

    public static int resolveSuccessStatus(OperationType operationType) {
        return resolveSuccessStatus(operationType, JsonApi4jCompatibilityMode.STRICT);
    }

    public static int resolveSuccessStatus(OperationType operationType,
                                           JsonApi4jCompatibilityMode compatibilityMode) {
        if (operationType == null) {
            throw new IllegalArgumentException("Operation type must not be null");
        }
        JsonApi4jCompatibilityMode effectiveMode = compatibilityMode == null
                ? JsonApi4jCompatibilityMode.STRICT
                : compatibilityMode;

        return switch (operationType) {
            case READ_RESOURCE_BY_ID,
                 READ_MULTIPLE_RESOURCES,
                 READ_TO_ONE_RELATIONSHIP,
                 READ_TO_MANY_RELATIONSHIP -> 200;
            case CREATE_RESOURCE -> 201;
            case UPDATE_RESOURCE,
                 DELETE_RESOURCE,
                 UPDATE_TO_ONE_RELATIONSHIP,
                 UPDATE_TO_MANY_RELATIONSHIP -> effectiveMode == JsonApi4jCompatibilityMode.LEGACY ? 202 : 204;
        };
    }

}
