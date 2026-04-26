package pro.api4.jsonapi4j.operation;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;

import java.util.Collections;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class OperationsRegistryReportGeneratorTests {

    private static final ResourceType USERS = new ResourceType("users");
    private static final RelationshipName CITIZENSHIPS = new RelationshipName("citizenships");

    // --- Empty registry ---

    @Test
    void emptyRegistry_reportsOperationsHeader() {
        // given
        OperationsRegistry registry = OperationsRegistry.empty();

        // when
        String report = new OperationsRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("--- Operations ---");
        assertThat(report).doesNotContain("users");
    }

    // --- Resource operations ---

    @Test
    void resourceWithReadById_reportsGetEndpoint() {
        // given
        OperationsRegistry registry = mock(OperationsRegistry.class);
        when(registry.getResourceTypesWithAnyOperationConfigured()).thenReturn(Set.of(USERS));
        when(registry.isResourceOperationConfigured(USERS, OperationType.READ_RESOURCE_BY_ID)).thenReturn(true);
        when(registry.getRelationshipNamesWithAnyOperationConfigured(USERS)).thenReturn(Collections.emptySet());

        // when
        String report = new OperationsRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("users:");
        assertThat(report).contains("GET /users/{id}");
    }

    @Test
    void resourceWithMultipleOperations_reportsAll() {
        // given
        OperationsRegistry registry = mock(OperationsRegistry.class);
        when(registry.getResourceTypesWithAnyOperationConfigured()).thenReturn(Set.of(USERS));
        when(registry.isResourceOperationConfigured(USERS, OperationType.READ_RESOURCE_BY_ID)).thenReturn(true);
        when(registry.isResourceOperationConfigured(USERS, OperationType.READ_MULTIPLE_RESOURCES)).thenReturn(true);
        when(registry.isResourceOperationConfigured(USERS, OperationType.CREATE_RESOURCE)).thenReturn(true);
        when(registry.isResourceOperationConfigured(USERS, OperationType.DELETE_RESOURCE)).thenReturn(true);
        when(registry.getRelationshipNamesWithAnyOperationConfigured(USERS)).thenReturn(Collections.emptySet());

        // when
        String report = new OperationsRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("GET /users/{id}");
        assertThat(report).contains("GET /users");
        assertThat(report).contains("POST /users");
        assertThat(report).contains("DELETE /users/{id}");
    }

    // --- Relationship operations ---

    @Test
    void resourceWithRelationshipOperation_reportsRelationshipEndpoint() {
        // given
        OperationsRegistry registry = mock(OperationsRegistry.class);
        when(registry.getResourceTypesWithAnyOperationConfigured()).thenReturn(Set.of(USERS));
        when(registry.getRelationshipNamesWithAnyOperationConfigured(USERS)).thenReturn(Set.of(CITIZENSHIPS));
        when(registry.isRelationshipOperationConfigured(USERS, CITIZENSHIPS, OperationType.READ_TO_MANY_RELATIONSHIP)).thenReturn(true);

        // when
        String report = new OperationsRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("GET /users/{id}/relationships/citizenships");
    }

}
