package pro.api4.jsonapi4j.domain;

import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DomainRegistryReportGeneratorTests {

    private static final ResourceType USERS = new ResourceType("users");
    private static final ResourceType COUNTRIES = new ResourceType("countries");
    private static final RelationshipName CITIZENSHIPS = new RelationshipName("citizenships");
    private static final RelationshipName PLACE_OF_BIRTH = new RelationshipName("placeOfBirth");

    // --- Empty registry ---

    @Test
    void emptyRegistry_reportsZeroResources() {
        // given
        DomainRegistry registry = DomainRegistry.empty();

        // when
        String report = new DomainRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("Resources (0)");
    }

    // --- Resources ---

    @Test
    void singleResource_reportsResourceTypeAndClass() {
        // given
        DomainRegistry registry = registryWithResources(USERS);

        // when
        String report = new DomainRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("Resources (1)");
        assertThat(report).contains("users");
    }

    @Test
    void multipleResources_reportsAll() {
        // given
        DomainRegistry registry = registryWithResources(USERS, COUNTRIES);

        // when
        String report = new DomainRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("Resources (2)");
        assertThat(report).contains("users");
        assertThat(report).contains("countries");
    }

    // --- Relationships ---

    @Test
    void resourceWithToOneRelationship_reportsRelationship() {
        // given
        DomainRegistry registry = registryWithToOneRelationship(USERS, PLACE_OF_BIRTH);

        // when
        String report = new DomainRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("placeOfBirth");
        assertThat(report).contains("[TO_ONE]");
    }

    @Test
    void resourceWithToManyRelationship_reportsRelationship() {
        // given
        DomainRegistry registry = registryWithToManyRelationship(USERS, CITIZENSHIPS);

        // when
        String report = new DomainRegistryReportGenerator(registry).generateStateReport();

        // then
        assertThat(report).contains("citizenships");
        assertThat(report).contains("[TO_MANY]");
    }

    // --- Helpers ---

    @SuppressWarnings("unchecked")
    private DomainRegistry registryWithResources(ResourceType... resourceTypes) {
        DomainRegistry registry = mock(DomainRegistry.class);
        var resources = new java.util.ArrayList<RegisteredResource<Resource<?>>>();
        for (ResourceType rt : resourceTypes) {
            RegisteredResource<Resource<?>> rr = RegisteredResource.<Resource<?>>builder()
                    .resource(mock(Resource.class))
                    .resourceType(rt)
                    .registeredAs(Resource.class)
                    .pluginInfo(Collections.emptyMap())
                    .build();
            resources.add(rr);
            when(registry.getToOneRelationships(rt)).thenReturn(Collections.emptyList());
            when(registry.getToManyRelationships(rt)).thenReturn(Collections.emptyList());
        }
        when(registry.getResources()).thenReturn(resources);
        return registry;
    }

    @SuppressWarnings("unchecked")
    private DomainRegistry registryWithToOneRelationship(ResourceType resourceType, RelationshipName relName) {
        DomainRegistry registry = mock(DomainRegistry.class);
        RegisteredResource<Resource<?>> rr = RegisteredResource.<Resource<?>>builder()
                .resource(mock(Resource.class))
                .resourceType(resourceType)
                .registeredAs(Resource.class)
                .pluginInfo(Collections.emptyMap())
                .build();
        RegisteredRelationship<ToOneRelationship<?>> rel = RegisteredRelationship.<ToOneRelationship<?>>builder()
                .relationship(mock(ToOneRelationship.class))
                .parentResourceType(resourceType)
                .relationshipName(relName)
                .relationshipType(RelationshipType.TO_ONE)
                .pluginInfo(Collections.emptyMap())
                .build();
        when(registry.getResources()).thenReturn(java.util.List.of(rr));
        when(registry.getToOneRelationships(resourceType)).thenReturn(java.util.List.of(rel));
        when(registry.getToManyRelationships(resourceType)).thenReturn(Collections.emptyList());
        return registry;
    }

    @SuppressWarnings("unchecked")
    private DomainRegistry registryWithToManyRelationship(ResourceType resourceType, RelationshipName relName) {
        DomainRegistry registry = mock(DomainRegistry.class);
        RegisteredResource<Resource<?>> rr = RegisteredResource.<Resource<?>>builder()
                .resource(mock(Resource.class))
                .resourceType(resourceType)
                .registeredAs(Resource.class)
                .pluginInfo(Collections.emptyMap())
                .build();
        RegisteredRelationship<ToManyRelationship<?>> rel = RegisteredRelationship.<ToManyRelationship<?>>builder()
                .relationship(mock(ToManyRelationship.class))
                .parentResourceType(resourceType)
                .relationshipName(relName)
                .relationshipType(RelationshipType.TO_MANY)
                .pluginInfo(Collections.emptyMap())
                .build();
        when(registry.getResources()).thenReturn(java.util.List.of(rr));
        when(registry.getToOneRelationships(resourceType)).thenReturn(Collections.emptyList());
        when(registry.getToManyRelationships(resourceType)).thenReturn(java.util.List.of(rel));
        return registry;
    }

}
