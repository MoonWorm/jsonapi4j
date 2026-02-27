package pro.api4.jsonapi4j.servlet.response;

import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.operation.OperationType;

public class OperationStatusResolver {

    @SuppressWarnings("unused")
    // Reserved for strict/legacy status branching in subsequent parity phases.
    private final JsonApi4jCompatibilityMode compatibilityMode;

    public OperationStatusResolver(JsonApi4jCompatibilityMode compatibilityMode) {
        this.compatibilityMode = compatibilityMode == null
                ? JsonApi4jCompatibilityMode.STRICT
                : compatibilityMode;
    }

    public int resolve(OperationType operationType) {
        return operationType.getHttpStatus();
    }
}
