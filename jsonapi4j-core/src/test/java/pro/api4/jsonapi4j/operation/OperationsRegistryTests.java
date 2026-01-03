package pro.api4.jsonapi4j.operation;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;

import java.util.Collections;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class OperationsRegistryTests {

    @Test
    public void empty_checkAllMethodsWorksAsExpected() {
        // given - when
        OperationsRegistry sut = OperationsRegistry.empty();

        // then
        assertThat(sut.getAllOperations()).isNotNull().isEmpty();
    }

    @Test
    public void registerSomeOperations_checkAllMethodsWorksAsExpected() {
        // given - when
        TestReadByIdOperation readByIdOperation = new TestReadByIdOperation();
        TestReadMultipleResourcesOperation readMultipleResourcesOperation = new TestReadMultipleResourcesOperation();
        TestCreateResourceOperation createResourceOperation = new TestCreateResourceOperation();
        TestUpdateResourceOperation updateResourceOperation = new TestUpdateResourceOperation();
        TestDeleteResourceOperation deleteResourceOperation = new TestDeleteResourceOperation();
        TestReadToOneRelationshipOperation readToOneRelationshipOperation = new TestReadToOneRelationshipOperation();
        TestReadToManyRelationshipOperation readToManyRelationshipOperation = new TestReadToManyRelationshipOperation();
        TestUpdateToOneRelationshipOperation updateToOneRelationshipOperation = new TestUpdateToOneRelationshipOperation();
        TestUpdateToManyRelationshipOperation updateToManyRelationshipOperation = new TestUpdateToManyRelationshipOperation();
        OperationsRegistry sut = OperationsRegistry.builder(Collections.emptyList())
                .operation(readByIdOperation)
                .operation(readMultipleResourcesOperation)
                .operation(createResourceOperation)
                .operation(updateResourceOperation)
                .operation(deleteResourceOperation)
                .operation(readToOneRelationshipOperation)
                .operation(readToManyRelationshipOperation)
                .operation(updateToOneRelationshipOperation)
                .operation(updateToManyRelationshipOperation)
                .build();

        // then
        ResourceType fooResource = new ResourceType("foo");
        RelationshipName toOneRelationship = new RelationshipName("to1");
        RelationshipName toManyRelationship = new RelationshipName("to2");
        assertThat(sut.getAllOperations()).isNotNull().hasSize(9);
        assertThat(sut.getResourceTypesWithAnyOperationConfigured()).isNotNull().isEqualTo(Set.of(fooResource));
        assertThat(sut.getRelationshipNamesWithAnyOperationConfigured(fooResource)).isEqualTo(Set.of(toOneRelationship, toManyRelationship));
        assertThat(sut.getRelationshipNamesWithAnyOperationConfigured(new ResourceType("non-existing"))).isEmpty();

        assertThat(sut.isRelationshipOperationConfigured(fooResource, toOneRelationship, OperationType.READ_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toOneRelationship, OperationType.UPDATE_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.READ_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.UPDATE_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.READ_RESOURCE_BY_ID)).isFalse();

        assertThat(sut.isAnyResourceOperationConfigured(fooResource)).isTrue();
        assertThat(sut.isAnyResourceOperationConfigured(new ResourceType("non-existing"))).isFalse();

        assertThat(sut.isAnyToOneRelationshipOperationConfigured(fooResource)).isTrue();
        assertThat(sut.isAnyToOneRelationshipOperationConfigured(new ResourceType("non-existing"))).isFalse();
        assertThat(sut.isAnyToOneRelationshipOperationConfigured(fooResource, toOneRelationship)).isTrue();
        assertThat(sut.isAnyToOneRelationshipOperationConfigured(fooResource, new RelationshipName("non-existing"))).isFalse();
        assertThat(sut.isToOneRelationshipOperationConfigured(fooResource, toOneRelationship, OperationType.READ_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isToOneRelationshipOperationConfigured(fooResource, toOneRelationship, OperationType.UPDATE_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isToOneRelationshipOperationConfigured(fooResource, new RelationshipName("non-existing"), OperationType.READ_TO_ONE_RELATIONSHIP)).isFalse();
        assertThat(sut.isToOneRelationshipOperationConfigured(fooResource, toOneRelationship, OperationType.READ_RESOURCE_BY_ID)).isFalse();

        assertThat(sut.isAnyToManyRelationshipOperationConfigured(fooResource)).isTrue();
        assertThat(sut.isAnyToManyRelationshipOperationConfigured(new ResourceType("non-existing"))).isFalse();
        assertThat(sut.isAnyToManyRelationshipOperationConfigured(fooResource, toManyRelationship)).isTrue();
        assertThat(sut.isAnyToManyRelationshipOperationConfigured(fooResource, new RelationshipName("non-existing"))).isFalse();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.READ_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.UPDATE_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, new RelationshipName("non-existing"), OperationType.READ_TO_MANY_RELATIONSHIP)).isFalse();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.READ_RESOURCE_BY_ID)).isFalse();

        assertThat(sut.getReadResourceByIdOperation(fooResource, false)).isNotNull().isEqualTo(readByIdOperation);
        assertThat(sut.getReadResourceByIdOperation(fooResource, true)).isNotNull().isEqualTo(readByIdOperation);
        assertThat(sut.getReadResourceByIdOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getReadResourceByIdOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getReadMultipleResourcesOperation(fooResource, false)).isNotNull().isEqualTo(readMultipleResourcesOperation);
        assertThat(sut.getReadMultipleResourcesOperation(fooResource, true)).isNotNull().isEqualTo(readMultipleResourcesOperation);
        assertThat(sut.getReadMultipleResourcesOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getReadMultipleResourcesOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getCreateResourceOperation(fooResource, false)).isNotNull().isEqualTo(createResourceOperation);
        assertThat(sut.getCreateResourceOperation(fooResource, true)).isNotNull().isEqualTo(createResourceOperation);
        assertThat(sut.getCreateResourceOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getCreateResourceOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getUpdateResourceOperation(fooResource, false)).isNotNull().isEqualTo(updateResourceOperation);
        assertThat(sut.getUpdateResourceOperation(fooResource, true)).isNotNull().isEqualTo(updateResourceOperation);
        assertThat(sut.getUpdateResourceOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getUpdateResourceOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getDeleteResourceOperation(fooResource, false)).isNotNull().isEqualTo(deleteResourceOperation);
        assertThat(sut.getDeleteResourceOperation(fooResource, true)).isNotNull().isEqualTo(deleteResourceOperation);
        assertThat(sut.getDeleteResourceOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getDeleteResourceOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getReadToOneRelationshipOperation(fooResource, toOneRelationship, false)).isNotNull().isEqualTo(readToOneRelationshipOperation);
        assertThat(sut.getReadToOneRelationshipOperation(fooResource, toOneRelationship, true)).isNotNull().isEqualTo(readToOneRelationshipOperation);
        assertThat(sut.getReadToOneRelationshipOperation(new ResourceType("non-existing"), toOneRelationship,false)).isNull();
        assertThat(sut.getReadToOneRelationshipOperation(fooResource, toManyRelationship,false)).isNull();
        assertThatThrownBy(() -> sut.getReadToOneRelationshipOperation(new ResourceType("non-existing"), toOneRelationship,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getReadToOneRelationshipOperation(fooResource, toManyRelationship,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getReadToManyRelationshipOperation(fooResource, toManyRelationship, false)).isNotNull().isEqualTo(readToManyRelationshipOperation);
        assertThat(sut.getReadToManyRelationshipOperation(fooResource, toManyRelationship, true)).isNotNull().isEqualTo(readToManyRelationshipOperation);
        assertThat(sut.getReadToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship,false)).isNull();
        assertThat(sut.getReadToManyRelationshipOperation(fooResource, toOneRelationship,false)).isNull();
        assertThatThrownBy(() -> sut.getReadToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getReadToManyRelationshipOperation(fooResource, toOneRelationship,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getUpdateToOneRelationshipOperation(fooResource, toOneRelationship, false)).isNotNull().isEqualTo(updateToOneRelationshipOperation);
        assertThat(sut.getUpdateToOneRelationshipOperation(fooResource, toOneRelationship, true)).isNotNull().isEqualTo(updateToOneRelationshipOperation);
        assertThat(sut.getUpdateToOneRelationshipOperation(new ResourceType("non-existing"), toOneRelationship,false)).isNull();
        assertThat(sut.getUpdateToOneRelationshipOperation(fooResource, toManyRelationship,false)).isNull();
        assertThatThrownBy(() -> sut.getUpdateToOneRelationshipOperation(new ResourceType("non-existing"), toOneRelationship,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getUpdateToOneRelationshipOperation(fooResource, toManyRelationship,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getUpdateToManyRelationshipOperation(fooResource, toManyRelationship, false)).isNotNull().isEqualTo(updateToManyRelationshipOperation);
        assertThat(sut.getUpdateToManyRelationshipOperation(fooResource, toManyRelationship, true)).isNotNull().isEqualTo(updateToManyRelationshipOperation);
        assertThat(sut.getUpdateToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship,false)).isNull();
        assertThat(sut.getUpdateToManyRelationshipOperation(fooResource, toOneRelationship,false)).isNull();
        assertThatThrownBy(() -> sut.getUpdateToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getUpdateToManyRelationshipOperation(fooResource, toOneRelationship,true)).isInstanceOf(OperationNotFoundException.class);
    }
    
    @JsonApiResource(resourceType = "foo")
    private static class TestFooResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return dataSourceDto;
        }
    }
    
    @JsonApiRelationship(relationshipName = "to1", parentResource = TestFooResource.class)
    private static class TestToOneRelationship implements ToOneRelationship<String, String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return s;
        }
    }

    @JsonApiRelationship(relationshipName = "to2", parentResource = TestFooResource.class)
    private static class TestToManyRelationship implements ToManyRelationship<String, String> {

        @Override
        public String resolveResourceIdentifierType(String s) {
            return "foo";
        }

        @Override
        public String resolveResourceIdentifierId(String s) {
            return s;
        }
    }

    @JsonApiResourceOperation(resource = TestFooResource.class)
    private static class TestReadByIdOperation implements ReadResourceByIdOperation<String> {

        @Override
        public String readById(JsonApiRequest request) {
            return UUID.randomUUID().toString();
        }

    }

    @JsonApiResourceOperation(resource = TestFooResource.class)
    private static class TestReadMultipleResourcesOperation implements ReadMultipleResourcesOperation<String> {

        @Override
        public CursorPageableResponse<String> readPage(JsonApiRequest request) {
            return CursorPageableResponse.empty();
        }

    }

    @JsonApiResourceOperation(resource = TestFooResource.class)
    private static class TestCreateResourceOperation implements CreateResourceOperation<String> {

        @Override
        public String create(JsonApiRequest request) {
            return UUID.randomUUID().toString();
        }
    }

    @JsonApiResourceOperation(resource = TestFooResource.class)
    private static class TestUpdateResourceOperation implements UpdateResourceOperation {

        @Override
        public void update(JsonApiRequest request) {

        }
    }

    @JsonApiResourceOperation(resource = TestFooResource.class)
    private static class TestDeleteResourceOperation implements DeleteResourceOperation {

        @Override
        public void delete(JsonApiRequest request) {

        }
    }

    @JsonApiRelationshipOperation(resource = TestFooResource.class, relationship = TestToOneRelationship.class)
    private static class TestReadToOneRelationshipOperation implements ReadToOneRelationshipOperation<String, String> {

        @Override
        public String readOne(JsonApiRequest relationshipRequest) {
            return UUID.randomUUID().toString();
        }

    }

    @JsonApiRelationshipOperation(resource = TestFooResource.class, relationship = TestToManyRelationship.class)
    private static class TestReadToManyRelationshipOperation implements ReadToManyRelationshipOperation<String, String> {

        @Override
        public CursorPageableResponse<String> readMany(JsonApiRequest relationshipRequest) {
            return CursorPageableResponse.empty();
        }
    }

    @JsonApiRelationshipOperation(resource = TestFooResource.class, relationship = TestToOneRelationship.class)
    private static class TestUpdateToOneRelationshipOperation implements UpdateToOneRelationshipOperation {

        @Override
        public void update(JsonApiRequest request) {

        }
    }

    @JsonApiRelationshipOperation(resource = TestFooResource.class, relationship = TestToManyRelationship.class)
    private static class TestUpdateToManyRelationshipOperation implements UpdateToManyRelationshipOperation {

        @Override
        public void update(JsonApiRequest request) {

        }
    }

}
