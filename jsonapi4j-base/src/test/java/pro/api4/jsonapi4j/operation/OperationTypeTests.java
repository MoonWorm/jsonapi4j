package pro.api4.jsonapi4j.operation;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.operation.OperationType.Method;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pro.api4.jsonapi4j.operation.OperationType.*;

public class OperationTypeTests {

    // --- getHttpStatus ---

    @Test
    public void getHttpStatus_readResourceById_returns200() {
        assertThat(READ_RESOURCE_BY_ID.getHttpStatus()).isEqualTo(200);
    }

    @Test
    public void getHttpStatus_readMultipleResources_returns200() {
        assertThat(READ_MULTIPLE_RESOURCES.getHttpStatus()).isEqualTo(200);
    }

    @Test
    public void getHttpStatus_createResource_returns201() {
        assertThat(CREATE_RESOURCE.getHttpStatus()).isEqualTo(201);
    }

    @Test
    public void getHttpStatus_updateResource_returns202() {
        assertThat(UPDATE_RESOURCE.getHttpStatus()).isEqualTo(202);
    }

    @Test
    public void getHttpStatus_deleteResource_returns202() {
        assertThat(DELETE_RESOURCE.getHttpStatus()).isEqualTo(202);
    }

    @Test
    public void getHttpStatus_readToOneRelationship_returns200() {
        assertThat(READ_TO_ONE_RELATIONSHIP.getHttpStatus()).isEqualTo(200);
    }

    @Test
    public void getHttpStatus_updateToOneRelationship_returns202() {
        assertThat(UPDATE_TO_ONE_RELATIONSHIP.getHttpStatus()).isEqualTo(202);
    }

    @Test
    public void getHttpStatus_readToManyRelationship_returns200() {
        assertThat(READ_TO_MANY_RELATIONSHIP.getHttpStatus()).isEqualTo(200);
    }

    @Test
    public void getHttpStatus_updateToManyRelationship_returns202() {
        assertThat(UPDATE_TO_MANY_RELATIONSHIP.getHttpStatus()).isEqualTo(202);
    }

    // --- Method.fromString ---

    @Test
    public void methodFromString_validUppercase_returnsMethod() {
        // when / then
        assertThat(Method.fromString("GET")).isEqualTo(Method.GET);
    }

    @Test
    public void methodFromString_validLowercase_returnsMethod() {
        // when / then
        assertThat(Method.fromString("get")).isEqualTo(Method.GET);
    }

    @Test
    public void methodFromString_invalidMethod_throwsException() {
        // when / then
        assertThatThrownBy(() -> Method.fromString("PUT"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    // --- Method.isSupportedMethod ---

    @Test
    public void isSupportedMethod_supportedMethod_returnsTrue() {
        // when / then
        assertThat(Method.isSupportedMethod("POST")).isTrue();
    }

    @Test
    public void isSupportedMethod_unsupportedMethod_returnsFalse() {
        // when / then
        assertThat(Method.isSupportedMethod("PUT")).isFalse();
    }

    @Test
    public void isSupportedMethod_caseInsensitive_returnsTrue() {
        // when / then
        assertThat(Method.isSupportedMethod("delete")).isTrue();
    }

    // --- Static filter methods ---

    @Test
    public void getResourceOperationTypes_returnsFiveResourceOps() {
        // when
        List<OperationType> result = getResourceOperationTypes();

        // then
        assertThat(result).hasSize(5)
                .containsExactly(
                        READ_RESOURCE_BY_ID,
                        READ_MULTIPLE_RESOURCES,
                        CREATE_RESOURCE,
                        UPDATE_RESOURCE,
                        DELETE_RESOURCE
                );
    }

    @Test
    public void getToOneRelationshipOperationTypes_returnsTwoOps() {
        // when
        List<OperationType> result = getToOneRelationshipOperationTypes();

        // then
        assertThat(result).hasSize(2)
                .containsExactly(READ_TO_ONE_RELATIONSHIP, UPDATE_TO_ONE_RELATIONSHIP);
    }

    @Test
    public void getToManyRelationshipOperationTypes_returnsTwoOps() {
        // when
        List<OperationType> result = getToManyRelationshipOperationTypes();

        // then
        assertThat(result).hasSize(2)
                .containsExactly(READ_TO_MANY_RELATIONSHIP, UPDATE_TO_MANY_RELATIONSHIP);
    }

    @Test
    public void getAllRelationshipOperationTypes_returnsFourOps() {
        // when
        List<OperationType> result = getAllRelationshipOperationTypes();

        // then
        assertThat(result).hasSize(4)
                .containsExactly(
                        READ_TO_ONE_RELATIONSHIP,
                        UPDATE_TO_ONE_RELATIONSHIP,
                        READ_TO_MANY_RELATIONSHIP,
                        UPDATE_TO_MANY_RELATIONSHIP
                );
    }

    @Test
    public void getExistingResourceAwareOperations_returnsSevenOps() {
        // when
        List<OperationType> result = getExistingResourceAwareOperations();

        // then
        assertThat(result).hasSize(7)
                .doesNotContain(READ_MULTIPLE_RESOURCES, CREATE_RESOURCE);
    }

}
