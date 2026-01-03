package pro.api4.jsonapi4j.domain;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class DomainRegistryTests {

    @Test
    public void empty_checkAllMethodsWorksAsExpected() {
        // given - when
        DomainRegistry sut = DomainRegistry.empty();

        // then
        assertThat(sut.getResources()).isNotNull().isEmpty();
        assertThat(sut.getResourceTypes()).isNotNull().isEmpty();
    }

    @Test
    public void resourceAndNoRelationships_checkAllMethodsWorksAsExpected() {
        // given - when
        TestResource testResource = new TestResource();
        DomainRegistry sut = DomainRegistry.builder(Collections.emptyList())
                .resource(testResource)
                .build();

        // then
        assertThat(sut.getResources()).isNotNull().hasSize(1);
        assertThat(sut.getResourceTypes()).isNotNull().hasSize(1);
        assertThat(sut.getResource(new ResourceType("foo")).getResource()).isEqualTo(testResource);
    }

    @Test
    public void resourceAndRelationships_checkAllMethodsWorksAsExpected() {
        // given - when
        TestResource testResource = new TestResource();
        TestToOneRelationship testToOneRelationship = new TestToOneRelationship();
        TestToManyRelationship testToManyRelationship = new TestToManyRelationship();
        DomainRegistry sut = DomainRegistry.builder(Collections.emptyList())
                .resource(testResource)
                .relationship(testToOneRelationship)
                .relationship(testToManyRelationship)
                .build();

        // then
        assertThat(sut.getResources()).isNotNull().hasSize(1);
        assertThat(sut.getResourceTypes()).isNotNull().hasSize(1);
        assertThat(sut.getResource(new ResourceType("foo")).getResource()).isEqualTo(testResource);

        assertThat(sut.getAvailableRelationshipNames(new ResourceType("foo"))).isNotNull().hasSize(2);

        assertThat(sut.getToOneRelationshipNames(new ResourceType("foo"))).isEqualTo(Set.of(new RelationshipName("to1")));
        assertThat(sut.getToOneRelationships(new ResourceType("foo")).stream().map(RegisteredRelationship::getRelationship).toList()).isEqualTo(List.of(testToOneRelationship));
        assertThat(sut.getToOneRelationshipStrict(new ResourceType("foo"), new RelationshipName("to1")).getRelationship()).isNotNull().isEqualTo(testToOneRelationship);
        assertThatThrownBy(() -> sut.getToOneRelationshipStrict(new ResourceType("foo"), new RelationshipName("to2"))).isInstanceOf(DomainMisconfigurationException.class);
        assertThatThrownBy(() -> sut.getToOneRelationshipStrict(new ResourceType("foo"), new RelationshipName("smth_else"))).isInstanceOf(DomainMisconfigurationException.class);
        assertThatThrownBy(() -> sut.getToOneRelationshipStrict(new ResourceType("bar"), new RelationshipName("smth_else"))).isInstanceOf(DomainMisconfigurationException.class);

        assertThat(sut.getToManyRelationshipNames(new ResourceType("foo"))).isEqualTo(Set.of(new RelationshipName("to2")));
        assertThat(sut.getToManyRelationships(new ResourceType("foo")).stream().map(RegisteredRelationship::getRelationship).toList()).isEqualTo(List.of(testToManyRelationship));
        assertThat(sut.getToManyRelationshipStrict(new ResourceType("foo"), new RelationshipName("to2")).getRelationship()).isNotNull().isEqualTo(testToManyRelationship);
        assertThatThrownBy(() -> sut.getToManyRelationshipStrict(new ResourceType("foo"), new RelationshipName("to1"))).isInstanceOf(DomainMisconfigurationException.class);
        assertThatThrownBy(() -> sut.getToManyRelationshipStrict(new ResourceType("foo"), new RelationshipName("smth_else"))).isInstanceOf(DomainMisconfigurationException.class);
        assertThatThrownBy(() -> sut.getToManyRelationshipStrict(new ResourceType("bar"), new RelationshipName("smth_else"))).isInstanceOf(DomainMisconfigurationException.class);
    }

    @JsonApiResource(resourceType = "foo")
    private static class TestResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return UUID.randomUUID().toString();
        }

    }

    @JsonApiRelationship(relationshipName = "to1", parentResource = TestResource.class)
    private static class TestToOneRelationship implements ToOneRelationship<String, String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

    @JsonApiRelationship(relationshipName = "to2", parentResource = TestResource.class)
    private static class TestToManyRelationship implements ToManyRelationship<String, String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

}
