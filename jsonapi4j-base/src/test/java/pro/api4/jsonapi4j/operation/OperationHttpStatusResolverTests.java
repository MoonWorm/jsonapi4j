package pro.api4.jsonapi4j.operation;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;

import static org.assertj.core.api.Assertions.assertThat;

public class OperationHttpStatusResolverTests {

    @Test
    public void strictMode_returns204ForMutationOperations() {
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.UPDATE_RESOURCE,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(204);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.DELETE_RESOURCE,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(204);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.UPDATE_TO_ONE_RELATIONSHIP,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(204);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.UPDATE_TO_MANY_RELATIONSHIP,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(204);
    }

    @Test
    public void strictMode_returns200ForReadsAnd201ForCreate() {
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.READ_RESOURCE_BY_ID,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(200);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.READ_MULTIPLE_RESOURCES,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(200);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.READ_TO_ONE_RELATIONSHIP,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(200);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.READ_TO_MANY_RELATIONSHIP,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(200);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.CREATE_RESOURCE,
                JsonApi4jCompatibilityMode.STRICT
        )).isEqualTo(201);
    }

    @Test
    public void legacyMode_preserves202ForMutations() {
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.UPDATE_RESOURCE,
                JsonApi4jCompatibilityMode.LEGACY
        )).isEqualTo(202);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.DELETE_RESOURCE,
                JsonApi4jCompatibilityMode.LEGACY
        )).isEqualTo(202);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.UPDATE_TO_ONE_RELATIONSHIP,
                JsonApi4jCompatibilityMode.LEGACY
        )).isEqualTo(202);
        assertThat(OperationHttpStatusResolver.resolveSuccessStatus(
                OperationType.UPDATE_TO_MANY_RELATIONSHIP,
                JsonApi4jCompatibilityMode.LEGACY
        )).isEqualTo(202);
    }

    @Test
    public void operationTypeHttpStatusDefaultsToStrictMode() {
        assertThat(OperationType.UPDATE_RESOURCE.getHttpStatus()).isEqualTo(204);
        assertThat(OperationType.CREATE_RESOURCE.getHttpStatus()).isEqualTo(201);
        assertThat(OperationType.READ_RESOURCE_BY_ID.getHttpStatus()).isEqualTo(200);
    }
}
