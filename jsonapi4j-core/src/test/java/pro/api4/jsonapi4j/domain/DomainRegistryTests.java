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
    public void build_validateIntegrityForToOneRelationship_throwsException() {
        assertThatThrownBy(() -> DomainRegistry.builder(Collections.emptyList())
                .relationship(new TestToOneRelationship()).build())
                .isInstanceOf(DomainMisconfigurationException.class)
                .hasMessage("(TestToOneRelationship) relationship belongs to an unregistered (foo) resource. Please register (foo) resource or double-check if parent resource has been correctly specified.");
    }

    @Test
    public void build_validateIntegrityForToManyRelationship_throwsException() {
        assertThatThrownBy(() -> DomainRegistry.builder(Collections.emptyList())
                .relationship(new TestToManyRelationship()).build())
                .isInstanceOf(DomainMisconfigurationException.class)
                .hasMessage("(TestToManyRelationship) relationship belongs to an unregistered (foo) resource. Please register (foo) resource or double-check if parent resource has been correctly specified.");
    }

    @Test
    public void resource_missingAnnotation_throwsException() {
        assertThatThrownBy(() -> DomainRegistry.builder(Collections.emptyList())
                .resource(new TestResourceWithoutAnnotation()))
                .isInstanceOf(DomainMisconfigurationException.class)
                .hasMessage("Each resource implementation must has @JsonApiResource annotation placed on the type level.");
    }

    @Test
    public void relationship_toOneMissingAnnotation_throwsException() {
        assertThatThrownBy(() -> DomainRegistry.builder(Collections.emptyList())
                .relationship(new TestToManyRelationshipWithoutAnnotation()))
                .isInstanceOf(DomainMisconfigurationException.class)
                .hasMessage("Each relationship implementation must has @JsonApiRelationship annotation placed on the type level.");
    }

    @Test
    public void relationship_toManyMissingAnnotation_throwsException() {
        assertThatThrownBy(() -> DomainRegistry.builder(Collections.emptyList())
                .relationship(new TestToManyRelationshipWithoutAnnotation()))
                .isInstanceOf(DomainMisconfigurationException.class)
                .hasMessage("Each relationship implementation must has @JsonApiRelationship annotation placed on the type level.");
    }

    @Test
    public void resource_multipleWithTheSameName_throwsException() {
        // given - when
        assertThatThrownBy(() -> DomainRegistry.builder(Collections.emptyList())
                .resource(new TestResourceFoo())
                .resource(new TestResourceSimilarFoo()))
                .isInstanceOf(DomainMisconfigurationException.class)
                .hasMessage("Multiple resource declarations found for (foO) resource type");
    }

    @Test
    public void relationship_multipleToOneWithTheSameName_throwsException() {
        // given - when
        assertThatThrownBy(() -> DomainRegistry.builder(Collections.emptyList())
                .relationship(new TestToOneRelationship())
                .relationship(new TestToOneRelationshipSimilar()))
                .isInstanceOf(DomainMisconfigurationException.class)
                .hasMessage("Multiple similar (tO1) relationship declarations found for (foo) resource type");
    }

    @Test
    public void relationship_multipleToManyWithTheSameName_throwsException() {
        // given - when
        assertThatThrownBy(() -> DomainRegistry.builder(Collections.emptyList())
                .relationship(new TestToManyRelationship())
                .relationship(new TestToManyRelationshipSimilar()))
                .isInstanceOf(DomainMisconfigurationException.class)
                .hasMessage("Multiple similar (tO2) relationship declarations found for (foo) resource type");
    }

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
        TestResourceFoo testResource = new TestResourceFoo();
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
        TestResourceFoo testResource = new TestResourceFoo();
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

        assertThat(sut.getRelationshipNames(new ResourceType("foo"))).isNotNull().hasSize(2);

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

    private static class TestResourceWithoutAnnotation implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return UUID.randomUUID().toString();
        }

    }

    @JsonApiResource(resourceType = "foo")
    private static class TestResourceFoo implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return UUID.randomUUID().toString();
        }

    }

    @JsonApiResource(resourceType = "foO")
    private static class TestResourceSimilarFoo implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return UUID.randomUUID().toString();
        }

    }

    private static class TestToOneRelationshipWithoutAnnotation implements ToOneRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

    @JsonApiRelationship(relationshipName = "to1", parentResource = TestResourceFoo.class)
    private static class TestToOneRelationship implements ToOneRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

    @JsonApiRelationship(relationshipName = "tO1", parentResource = TestResourceFoo.class)
    private static class TestToOneRelationshipSimilar implements ToOneRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

    private static class TestToManyRelationshipWithoutAnnotation implements ToManyRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

    @JsonApiRelationship(relationshipName = "to2", parentResource = TestResourceFoo.class)
    private static class TestToManyRelationship implements ToManyRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

    @JsonApiRelationship(relationshipName = "tO2", parentResource = TestResourceFoo.class)
    private static class TestToManyRelationshipSimilar implements ToManyRelationship<String> {

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
