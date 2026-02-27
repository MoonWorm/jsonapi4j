package pro.api4.jsonapi4j.servlet.request;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.http.exception.MethodNotSupportedException;
import pro.api4.jsonapi4j.operation.OperationType;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OperationDetailsResolverTests {

    @Test
    public void toManyRelationshipSupportsGetPatchPostDelete() {
        OperationDetailsResolver sut = new OperationDetailsResolver(testDomainRegistry());

        OperationDetailsResolver.OperationDetails getDetails = sut.fromUrlAndMethod("/foos/1/relationships/bars", "GET");
        OperationDetailsResolver.OperationDetails patchDetails = sut.fromUrlAndMethod("/foos/1/relationships/bars", "PATCH");
        OperationDetailsResolver.OperationDetails postDetails = sut.fromUrlAndMethod("/foos/1/relationships/bars", "POST");
        OperationDetailsResolver.OperationDetails deleteDetails = sut.fromUrlAndMethod("/foos/1/relationships/bars", "DELETE");

        assertOperation(getDetails, OperationType.READ_TO_MANY_RELATIONSHIP);
        assertOperation(patchDetails, OperationType.UPDATE_TO_MANY_RELATIONSHIP);
        assertOperation(postDetails, OperationType.ADD_TO_MANY_RELATIONSHIP);
        assertOperation(deleteDetails, OperationType.REMOVE_FROM_MANY_RELATIONSHIP);
    }

    @Test
    public void toOneRelationshipRemainsGetPatchOnly() {
        OperationDetailsResolver sut = new OperationDetailsResolver(testDomainRegistry());

        OperationDetailsResolver.OperationDetails getDetails = sut.fromUrlAndMethod("/foos/1/relationships/baz", "GET");
        OperationDetailsResolver.OperationDetails patchDetails = sut.fromUrlAndMethod("/foos/1/relationships/baz", "PATCH");

        assertOperation(getDetails, OperationType.READ_TO_ONE_RELATIONSHIP);
        assertOperation(patchDetails, OperationType.UPDATE_TO_ONE_RELATIONSHIP);

        assertThatThrownBy(() -> sut.fromUrlAndMethod("/foos/1/relationships/baz", "POST"))
                .isInstanceOf(MethodNotSupportedException.class)
                .hasMessageContaining("GET, PATCH");
        assertThatThrownBy(() -> sut.fromUrlAndMethod("/foos/1/relationships/baz", "DELETE"))
                .isInstanceOf(MethodNotSupportedException.class)
                .hasMessageContaining("GET, PATCH");
    }

    private static void assertOperation(OperationDetailsResolver.OperationDetails details,
                                        OperationType expectedOperationType) {
        assertThat(details.getOperationType()).isEqualTo(expectedOperationType);
        assertThat(details.getResourceType()).isEqualTo(new ResourceType("foos"));
        assertThat(details.getRelationshipName()).isEqualTo(new RelationshipName(expectedOperationType.getSubType() == OperationType.SubType.TO_MANY_RELATIONSHIP ? "bars" : "baz"));
    }

    private static DomainRegistry testDomainRegistry() {
        return DomainRegistry.builder(Collections.emptyList())
                .resource(new TestFooResource())
                .relationship(new TestBarsRelationship())
                .relationship(new TestBazRelationship())
                .build();
    }

    @JsonApiResource(resourceType = "foos")
    private static class TestFooResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return dataSourceDto;
        }
    }

    @JsonApiRelationship(relationshipName = "bars", parentResource = TestFooResource.class)
    private static class TestBarsRelationship implements ToManyRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String relationshipDto) {
            return "foos";
        }

        @Override
        public String resolveResourceIdentifierId(String relationshipDto) {
            return relationshipDto;
        }
    }

    @JsonApiRelationship(relationshipName = "baz", parentResource = TestFooResource.class)
    private static class TestBazRelationship implements ToOneRelationship<String> {

        @Override
        public String resolveResourceIdentifierType(String relationshipDto) {
            return "foos";
        }

        @Override
        public String resolveResourceIdentifierId(String relationshipDto) {
            return relationshipDto;
        }
    }

}
