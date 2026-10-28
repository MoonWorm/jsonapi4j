package pro.api4.jsonapi4j;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.meta.MetaDoc;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EntryPointDocComposerTests {

    private static final ResourceType USERS = new ResourceType("users");
    private static final ResourceType COUNTRIES = new ResourceType("countries");
    private static final ResourceType STATE = new ResourceType("state");
    private static final ResourceType OPERATIONS = new ResourceType("operations");

    private DomainRegistry domainRegistry;
    private OperationsRegistry operationsRegistry;
    private EntryPointDocComposer sut;

    @BeforeEach
    void setUp() {
        domainRegistry = mock(DomainRegistry.class);
        operationsRegistry = mock(OperationsRegistry.class);
        sut = new EntryPointDocComposer(domainRegistry, operationsRegistry);
    }

    private void givenResourceTypes(ResourceType... resourceTypes) {
        when(domainRegistry.getResourceTypes()).thenReturn(new LinkedHashSet<>(List.of(resourceTypes)));
    }

    private void givenReadAllConfigured(ResourceType... resourceTypes) {
        when(operationsRegistry.isResourceOperationConfigured(any(), eq(OperationType.READ_MULTIPLE_RESOURCES)))
                .thenAnswer(invocation -> List.of(resourceTypes).contains(invocation.<ResourceType>getArgument(0)));
    }

    @Nested
    class Links {

        @Test
        void compose_noResources_containsOnlySelfLink() {
            givenResourceTypes();
            givenReadAllConfigured();

            MetaDoc doc = sut.compose();

            assertThat(doc.getLinks()).containsExactly(entry("self", "/"));
        }

        @Test
        void compose_resourcesWithReadAll_linksEachCollectionSortedByType() {
            givenResourceTypes(USERS, COUNTRIES);
            givenReadAllConfigured(USERS, COUNTRIES);

            MetaDoc doc = sut.compose();

            assertThat(doc.getLinks()).containsExactly(
                    entry("self", "/"),
                    entry("countries", "/countries"),
                    entry("users", "/users")
            );
        }

        @Test
        void compose_resourceWithoutReadAll_isNotLinked() {
            givenResourceTypes(USERS, COUNTRIES);
            givenReadAllConfigured(USERS);

            MetaDoc doc = sut.compose();

            assertThat(doc.getLinks()).containsOnlyKeys("self", "users");
        }

    }

    @Nested
    class MetaApi {

        @Test
        void compose_metaEnabled_linksStateSingletonInsteadOfMetaCollections() {
            givenResourceTypes(USERS, STATE, OPERATIONS);
            givenReadAllConfigured(USERS, STATE, OPERATIONS);
            when(domainRegistry.isMetaEnabled()).thenReturn(true);

            MetaDoc doc = sut.compose();

            assertThat(doc.getLinks()).containsExactly(
                    entry("self", "/"),
                    entry("users", "/users"),
                    entry("meta", "/state/this")
            );
        }

        @Test
        void compose_metaDisabled_hasNoMetaLink() {
            givenResourceTypes(USERS);
            givenReadAllConfigured(USERS);
            when(domainRegistry.isMetaEnabled()).thenReturn(false);

            MetaDoc doc = sut.compose();

            assertThat(doc.getLinks()).doesNotContainKey("meta");
        }

    }

    @Nested
    class TopLevelMembers {

        @Test
        void compose_always_hasEmptyMetaAndJsonApiVersion() {
            givenResourceTypes(USERS);
            givenReadAllConfigured(USERS);

            MetaDoc doc = sut.compose();

            assertThat(doc.getMeta()).isEqualTo(Map.of());
            assertThat(doc.getJsonapi().getVersion()).isEqualTo("1.1");
        }

    }

}
