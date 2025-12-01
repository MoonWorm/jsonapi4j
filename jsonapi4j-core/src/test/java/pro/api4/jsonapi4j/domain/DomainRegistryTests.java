package pro.api4.jsonapi4j.domain;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.exception.DomainMisconfigurationException;

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
        DomainRegistry sut = DomainRegistry.builder()
                .resource(testResource)
                .build();

        // then
        assertThat(sut.getResources()).isNotNull().hasSize(1);
        assertThat(sut.getResourceTypes()).isNotNull().hasSize(1);
        assertThat(sut.getResource(TestResourceTypes.FOO)).isEqualTo(testResource);
    }

    @Test
    public void resourceAndRelationships_checkAllMethodsWorksAsExpected() {
        // given - when
        TestResource testResource = new TestResource();
        TestToOneRelationship testToOneRelationship = new TestToOneRelationship();
        TestToManyRelationship testToManyRelationship = new TestToManyRelationship();
        DomainRegistry sut = DomainRegistry.builder()
                .resource(testResource)
                .relationship(testToOneRelationship)
                .relationship(testToManyRelationship)
                .build();

        // then
        assertThat(sut.getResources()).isNotNull().hasSize(1);
        assertThat(sut.getResourceTypes()).isNotNull().hasSize(1);
        assertThat(sut.getResource(TestResourceTypes.FOO)).isEqualTo(testResource);

        assertThat(sut.getAvailableRelationshipNames(TestResourceTypes.FOO)).isNotNull().hasSize(2);

        assertThat(sut.getToOneRelationshipNames(TestResourceTypes.FOO)).isEqualTo(Set.of(TestRelationships.TO_ONE));
        assertThat(sut.getToOneRelationships(TestResourceTypes.FOO)).isEqualTo(List.of(testToOneRelationship));
        assertThat(sut.getToOneRelationshipStrict(TestResourceTypes.FOO, TestRelationships.TO_ONE)).isNotNull().isEqualTo(testToOneRelationship);
        assertThatThrownBy(() -> sut.getToOneRelationshipStrict(TestResourceTypes.FOO, TestRelationships.TO_MANY)).isInstanceOf(DomainMisconfigurationException.class);
        assertThatThrownBy(() -> sut.getToOneRelationshipStrict(TestResourceTypes.FOO, TestRelationships.SOMETHING_ELSE)).isInstanceOf(DomainMisconfigurationException.class);
        assertThatThrownBy(() -> sut.getToOneRelationshipStrict(TestResourceTypes.BAR, TestRelationships.SOMETHING_ELSE)).isInstanceOf(DomainMisconfigurationException.class);

        assertThat(sut.getToManyRelationshipNames(TestResourceTypes.FOO)).isEqualTo(Set.of(TestRelationships.TO_MANY));
        assertThat(sut.getToManyRelationships(TestResourceTypes.FOO)).isEqualTo(List.of(testToManyRelationship));
        assertThat(sut.getToManyRelationshipStrict(TestResourceTypes.FOO, TestRelationships.TO_MANY)).isNotNull().isEqualTo(testToManyRelationship);
        assertThatThrownBy(() -> sut.getToManyRelationshipStrict(TestResourceTypes.FOO, TestRelationships.TO_ONE)).isInstanceOf(DomainMisconfigurationException.class);
        assertThatThrownBy(() -> sut.getToManyRelationshipStrict(TestResourceTypes.FOO, TestRelationships.SOMETHING_ELSE)).isInstanceOf(DomainMisconfigurationException.class);
        assertThatThrownBy(() -> sut.getToManyRelationshipStrict(TestResourceTypes.BAR, TestRelationships.SOMETHING_ELSE)).isInstanceOf(DomainMisconfigurationException.class);
    }

    private static class TestResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return UUID.randomUUID().toString();
        }

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }
    }

    private static class TestToOneRelationship implements ToOneRelationship<String, String> {

        @Override
        public RelationshipName relationshipName() {
            return TestRelationships.TO_ONE;
        }

        @Override
        public ResourceType parentResourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public ResourceType resolveResourceIdentifierType(String s) {
            return TestResourceTypes.FOO;
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

    private static class TestToManyRelationship implements ToManyRelationship<String, String> {

        @Override
        public RelationshipName relationshipName() {
            return TestRelationships.TO_MANY;
        }

        @Override
        public ResourceType parentResourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public ResourceType resolveResourceIdentifierType(String s) {
            return TestResourceTypes.FOO;
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return UUID.randomUUID().toString();
        }
    }

    public enum TestResourceTypes implements ResourceType {
        FOO("foo"),
        BAR("bar");

        private final String name;

        TestResourceTypes(String name) {
            this.name = name;
        }

        @Override
        public String getType() {
            return this.name;
        }
    }

    public enum TestRelationships implements RelationshipName {
        TO_ONE("to1"),
        TO_MANY("to2"),
        SOMETHING_ELSE("somethingElse");

        private final String name;

        TestRelationships(String name) {
            this.name = name;
        }

        @Override
        public String getName() {
            return name;
        }
    }
}
