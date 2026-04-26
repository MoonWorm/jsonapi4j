package pro.api4.jsonapi4j;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class JsonApi4jReportGeneratorTests {

    private static final ResourceType USERS = new ResourceType("users");
    private static final RelationshipName CITIZENSHIPS = new RelationshipName("citizenships");

    // --- Plugins section ---

    @Test
    void noPlugins_reportsZeroPlugins() {
        // given
        JsonApi4j jsonApi4j = buildJsonApi4j(Collections.emptyList());

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then
        assertThat(report).contains("Plugins (0)");
        assertThat(report).contains("(none)");
    }

    @Test
    void enabledPlugins_reportsPluginNames() {
        // given
        JsonApi4jPlugin plugin1 = mockPlugin("access-control", true);
        JsonApi4jPlugin plugin2 = mockPlugin("sparse-fieldsets", true);
        JsonApi4j jsonApi4j = buildJsonApi4j(List.of(plugin1, plugin2));

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then
        assertThat(report).contains("Plugins (2)");
        assertThat(report).contains("access-control");
        assertThat(report).contains("sparse-fieldsets");
    }

    @Test
    void disabledPlugin_notIncludedInReport() {
        // given
        JsonApi4jPlugin enabled = mockPlugin("access-control", true);
        JsonApi4jPlugin disabled = mockPlugin("openapi", false);
        JsonApi4j jsonApi4j = buildJsonApi4j(List.of(enabled, disabled));

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then
        assertThat(report).contains("Plugins (1)");
        assertThat(report).contains("access-control");
        assertThat(report).doesNotContain("openapi");
    }

    // --- Full report structure ---

    @Test
    void fullReport_containsAllSections() {
        // given
        JsonApi4j jsonApi4j = buildJsonApi4j(Collections.emptyList());

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then
        assertThat(report).contains("JsonApi4j State");
        assertThat(report).contains("Plugins");
        assertThat(report).contains("Resources");
        assertThat(report).contains("Operations");
    }

    // --- Warnings section ---

    @Test
    @SuppressWarnings("unchecked")
    void resourceWithoutOperations_reportsWarning() {
        // given
        DomainRegistry domainRegistry = mock(DomainRegistry.class);
        when(domainRegistry.getResourceTypes()).thenReturn(Set.of(USERS));
        when(domainRegistry.getResources()).thenReturn(List.of(
                RegisteredResource.<Resource<?>>builder()
                        .resource(mock(Resource.class))
                        .resourceType(USERS)
                        .registeredAs(Resource.class)
                        .pluginInfo(Collections.emptyMap())
                        .build()
        ));
        when(domainRegistry.getToOneRelationships(USERS)).thenReturn(Collections.emptyList());
        when(domainRegistry.getToManyRelationships(USERS)).thenReturn(Collections.emptyList());

        OperationsRegistry operationsRegistry = mock(OperationsRegistry.class);
        when(operationsRegistry.isAnyResourceOperationConfigured(USERS)).thenReturn(false);
        when(operationsRegistry.getResourceTypesWithAnyOperationConfigured()).thenReturn(Collections.emptySet());

        JsonApi4j jsonApi4j = JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .plugins(Collections.emptyList())
                .build();

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then
        assertThat(report).contains("Warnings");
        assertThat(report).contains("Resources without any operations implemented");
        assertThat(report).contains("users");
    }

    @Test
    @SuppressWarnings("unchecked")
    void toManyRelationshipWithoutOperations_reportsWarning() {
        // given
        DomainRegistry domainRegistry = mock(DomainRegistry.class);
        when(domainRegistry.getResourceTypes()).thenReturn(Set.of(USERS));
        when(domainRegistry.getResources()).thenReturn(List.of(
                RegisteredResource.<Resource<?>>builder()
                        .resource(mock(Resource.class))
                        .resourceType(USERS)
                        .registeredAs(Resource.class)
                        .pluginInfo(Collections.emptyMap())
                        .build()
        ));
        when(domainRegistry.getToOneRelationships(USERS)).thenReturn(Collections.emptyList());
        RegisteredRelationship<ToManyRelationship<?>> rel = RegisteredRelationship.<ToManyRelationship<?>>builder()
                .relationship(mock(ToManyRelationship.class))
                .parentResourceType(USERS)
                .relationshipName(CITIZENSHIPS)
                .relationshipType(RelationshipType.TO_MANY)
                .pluginInfo(Collections.emptyMap())
                .build();
        when(domainRegistry.getToManyRelationships(USERS)).thenReturn(List.of(rel));

        OperationsRegistry operationsRegistry = mock(OperationsRegistry.class);
        when(operationsRegistry.isAnyResourceOperationConfigured(USERS)).thenReturn(true);
        when(operationsRegistry.isAnyToManyRelationshipOperationConfigured(USERS, CITIZENSHIPS)).thenReturn(false);
        when(operationsRegistry.getResourceTypesWithAnyOperationConfigured()).thenReturn(Set.of(USERS));
        when(operationsRegistry.getRelationshipNamesWithAnyOperationConfigured(USERS)).thenReturn(Collections.emptySet());

        JsonApi4j jsonApi4j = JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .plugins(Collections.emptyList())
                .build();

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then
        assertThat(report).contains("Warnings");
        assertThat(report).contains("To-Many Relationships without any operations implemented");
        assertThat(report).contains("citizenships");
    }

    // --- Helpers ---

    private JsonApi4j buildJsonApi4j(List<JsonApi4jPlugin> plugins) {
        return JsonApi4j.builder()
                .plugins(plugins)
                .build();
    }

    private JsonApi4jPlugin mockPlugin(String name, boolean enabled) {
        JsonApi4jPlugin plugin = mock(JsonApi4jPlugin.class);
        when(plugin.pluginName()).thenReturn(name);
        when(plugin.enabled()).thenReturn(enabled);
        return plugin;
    }

}
