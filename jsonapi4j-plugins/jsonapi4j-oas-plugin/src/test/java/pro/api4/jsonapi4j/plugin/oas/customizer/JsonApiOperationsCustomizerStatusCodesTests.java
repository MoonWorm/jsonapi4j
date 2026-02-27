package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RegisteredResource;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.RegisteredOperation;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.operation.model.OasOperationInfoModel;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JsonApiOperationsCustomizerStatusCodesTests {

    @Test
    public void strictMode_uses204ForUpdateOperationInOas() {
        OpenAPI openAPI = new OpenAPI();

        new JsonApiOperationsCustomizer(
                "/jsonapi",
                mockDomainRegistryFor(new ResourceType("users")),
                mockOperationsRegistryForUpdate(new ResourceType("users")),
                Collections.emptyMap(),
                JsonApi4jCompatibilityMode.STRICT
        ).customise(openAPI);

        assertThat(openAPI.getPaths().get("/jsonapi/users/{id}").getPatch().getResponses())
                .containsKey("204")
                .doesNotContainKey("202");
    }

    @Test
    public void legacyMode_uses202ForUpdateOperationInOas() {
        OpenAPI openAPI = new OpenAPI();

        new JsonApiOperationsCustomizer(
                "/jsonapi",
                mockDomainRegistryFor(new ResourceType("users")),
                mockOperationsRegistryForUpdate(new ResourceType("users")),
                Collections.emptyMap(),
                JsonApi4jCompatibilityMode.LEGACY
        ).customise(openAPI);

        assertThat(openAPI.getPaths().get("/jsonapi/users/{id}").getPatch().getResponses())
                .containsKey("202")
                .doesNotContainKey("204");
    }

    @Test
    public void strictMode_usesPostDeleteForToManyLinkageOperationsInOas() {
        OpenAPI openAPI = new OpenAPI();
        ResourceType resourceType = new ResourceType("users");
        RelationshipName relationshipName = new RelationshipName("citizenships");

        new JsonApiOperationsCustomizer(
                "/jsonapi",
                mockDomainRegistryFor(resourceType),
                mockOperationsRegistryForToManyLinkage(resourceType, relationshipName),
                Collections.emptyMap(),
                JsonApi4jCompatibilityMode.STRICT
        ).customise(openAPI);

        assertThat(openAPI.getPaths().get("/jsonapi/users/{id}/relationships/citizenships").getPost().getResponses())
                .containsKey("204")
                .doesNotContainKey("202");
        assertThat(openAPI.getPaths().get("/jsonapi/users/{id}/relationships/citizenships").getDelete().getResponses())
                .containsKey("204")
                .doesNotContainKey("202");
    }

    private static DomainRegistry mockDomainRegistryFor(ResourceType resourceType) {
        DomainRegistry domainRegistry = mock(DomainRegistry.class);
        RegisteredResource<?> registeredResource = RegisteredResource.builder()
                .resourceType(resourceType)
                .registeredAs(Object.class)
                .pluginInfo(Collections.emptyMap())
                .build();
        when(domainRegistry.getResource(resourceType)).thenReturn((RegisteredResource) registeredResource);
        return domainRegistry;
    }

    private static OperationsRegistry mockOperationsRegistryForUpdate(ResourceType resourceType) {
        OperationsRegistry operationsRegistry = mock(OperationsRegistry.class);
        RegisteredOperation<?> registeredOperation = RegisteredOperation.builder()
                .registeredAs(Object.class)
                .resourceType(resourceType)
                .operationType(OperationType.UPDATE_RESOURCE)
                .pluginInfo(Map.of(JsonApiOasPlugin.NAME, OasOperationInfoModel.builder().build()))
                .build();

        when(operationsRegistry.getResourceTypesWithAnyOperationConfigured()).thenReturn(Set.of(resourceType));
        when(operationsRegistry.isResourceOperationConfigured(resourceType, OperationType.UPDATE_RESOURCE)).thenReturn(true);
        when(operationsRegistry.getRegisteredResourceOperation(resourceType, OperationType.UPDATE_RESOURCE, false))
                .thenReturn((RegisteredOperation) registeredOperation);
        when(operationsRegistry.getRelationshipNamesWithAnyOperationConfigured(resourceType)).thenReturn(Set.of());
        return operationsRegistry;
    }

    private static OperationsRegistry mockOperationsRegistryForToManyLinkage(ResourceType resourceType,
                                                                              RelationshipName relationshipName) {
        OperationsRegistry operationsRegistry = mock(OperationsRegistry.class);
        RegisteredOperation<?> addOperation = RegisteredOperation.builder()
                .registeredAs(Object.class)
                .resourceType(resourceType)
                .relationshipName(relationshipName)
                .operationType(OperationType.ADD_TO_MANY_RELATIONSHIP)
                .pluginInfo(Map.of(JsonApiOasPlugin.NAME, OasOperationInfoModel.builder().build()))
                .build();
        RegisteredOperation<?> removeOperation = RegisteredOperation.builder()
                .registeredAs(Object.class)
                .resourceType(resourceType)
                .relationshipName(relationshipName)
                .operationType(OperationType.REMOVE_FROM_MANY_RELATIONSHIP)
                .pluginInfo(Map.of(JsonApiOasPlugin.NAME, OasOperationInfoModel.builder().build()))
                .build();

        when(operationsRegistry.getResourceTypesWithAnyOperationConfigured()).thenReturn(Set.of(resourceType));
        when(operationsRegistry.getRelationshipNamesWithAnyOperationConfigured(resourceType)).thenReturn(Set.of(relationshipName));
        when(operationsRegistry.isRelationshipOperationConfigured(resourceType, relationshipName, OperationType.ADD_TO_MANY_RELATIONSHIP)).thenReturn(true);
        when(operationsRegistry.isRelationshipOperationConfigured(resourceType, relationshipName, OperationType.REMOVE_FROM_MANY_RELATIONSHIP)).thenReturn(true);
        when(operationsRegistry.getRegisteredRelationshipOperation(resourceType, relationshipName, OperationType.ADD_TO_MANY_RELATIONSHIP, false))
                .thenReturn((RegisteredOperation) addOperation);
        when(operationsRegistry.getRegisteredRelationshipOperation(resourceType, relationshipName, OperationType.REMOVE_FROM_MANY_RELATIONSHIP, false))
                .thenReturn((RegisteredOperation) removeOperation);
        return operationsRegistry;
    }
}
