package pro.api4.jsonapi4j;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.config.JsonApi4jProperties;
import pro.api4.jsonapi4j.meta.context.MetaContext;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        assertThat(report).contains("████████");
        assertThat(report).contains("Plugins");
        assertThat(report).contains("Domain");
        assertThat(report).contains("Operations");
    }

    // --- Meta API live URLs ---

    @Test
    void metaEnabled_rendersRelativeLinksDistributedAcrossSections() {
        // given — Compound Docs enabled, so the one-request ?include=… link resolves
        MetaContext metaContext = MetaContext.of(metaConfig(true), MetaContext.Integration.SPRING);
        JsonApi4j jsonApi4j = buildJsonApi4jWithMeta(metaContext);

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then — introspection links are rendered relative to the root path...
        assertThat(report).contains("Live introspection: /jsonapi/state/this?include=plugins,resources,relationships,operations,config");
        // ...plus a direct resource-facing link per section (not ?include=)
        assertThat(report).contains("(/jsonapi/plugins)");
        assertThat(report).contains("(/jsonapi/resources)");
        assertThat(report).contains("(/jsonapi/relationships)");
        assertThat(report).contains("(/jsonapi/operations)");
        // header-only Config section points at the singleton
        assertThat(report).contains("--- Config ---  (/jsonapi/config/this)");
    }

    @Test
    void metaEnabled_compoundDocsDisabled_rendersBareStateLinkWithNote() {
        // given — meta on, but Compound Docs off: ?include=… would be a no-op
        MetaContext metaContext = MetaContext.of(metaConfig(false), MetaContext.Integration.SPRING);
        JsonApi4j jsonApi4j = buildJsonApi4jWithMeta(metaContext);

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then — the banner link points at the bare singleton (no misleading ?include=…)...
        assertThat(report).contains("Live introspection: /jsonapi/state/this");
        assertThat(report).doesNotContain("state/this?include");
        // ...with a note steering users to the Compound Docs plugin and the per-section links
        assertThat(report).contains("require the Compound Docs plugin");
        assertThat(report).contains("jsonapi4j.cd.enabled=true");
        // the per-section links still render and are queryable directly
        assertThat(report).contains("(/jsonapi/plugins)");
        assertThat(report).contains("--- Config ---  (/jsonapi/config/this)");
    }

    @Test
    void metaDisabled_omitsLiveIntrospectionUrls() {
        // given
        JsonApi4j jsonApi4j = buildJsonApi4j(Collections.emptyList());

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then
        assertThat(report).doesNotContain("Live introspection");
        assertThat(report).doesNotContain("state/this?include");
        assertThat(report).doesNotContain("--- Config ---");
    }

    @Test
    void metaEnabled_excludesMetaFromDomainAndOperationsSections() {
        // given — a JsonApi4j whose only registered components are the built-in meta ones (Compound Docs enabled)
        MetaContext metaContext = MetaContext.of(metaConfig(true), MetaContext.Integration.SPRING);
        DomainRegistry domainRegistry = DomainRegistry.builder(Collections.emptyList()).build();
        OperationsRegistry operationsRegistry = OperationsRegistry.builder(Collections.emptyList()).build();
        JsonApi4j jsonApi4j = JsonApi4j.builder()
                .domainRegistry(domainRegistry)
                .operationsRegistry(operationsRegistry)
                .plugins(Collections.emptyList())
                .meta(metaContext)
                .build();

        // when
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then — the host-facing domain/operations sections are empty of meta noise...
        assertThat(report).contains("Domain (0)");
        assertThat(report).doesNotContain("(StateResource)");
        assertThat(report).doesNotContain("state:");
        // ...while the meta components remain documented via the live introspection links
        assertThat(report).contains("Live introspection: /jsonapi/state/this?include=plugins");
        assertThat(report).contains("--- Config ---  (/jsonapi/config/this)");
    }

    @Test
    void noMetaContext_omitsLiveIntrospectionUrls() {
        // given
        JsonApi4j jsonApi4j = buildJsonApi4j(Collections.emptyList());

        // when (legacy single-arg constructor)
        String report = new JsonApi4jReportGenerator(jsonApi4j).generateStateReport();

        // then
        assertThat(report).doesNotContain("Live introspection");
        assertThat(report).doesNotContain("--- Config ---");
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

    /**
     * Effective config snapshot for a meta-enabled context: the root path plus the {@code cd.enabled} flag the report
     * reads to decide whether {@code ?include=…} compound-document links resolve.
     */
    private static Map<String, Object> metaConfig(boolean compoundDocsEnabled) {
        return Map.of(
                JsonApi4jProperties.ROOT_PATH_PROPERTY, JsonApi4jProperties.DEFAULT_ROOT_PATH,
                "cd", Map.of("enabled", compoundDocsEnabled));
    }

    private JsonApi4j buildJsonApi4jWithMeta(MetaContext metaContext) {
        return JsonApi4j.builder()
                .plugins(Collections.emptyList())
                .meta(metaContext)
                .build();
    }

    private JsonApi4jPlugin mockPlugin(String name, boolean enabled) {
        JsonApi4jPlugin plugin = mock(JsonApi4jPlugin.class);
        when(plugin.pluginName()).thenReturn(name);
        when(plugin.enabled()).thenReturn(enabled);
        return plugin;
    }

}
