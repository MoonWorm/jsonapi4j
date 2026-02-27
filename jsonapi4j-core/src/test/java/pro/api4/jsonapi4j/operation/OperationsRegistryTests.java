package pro.api4.jsonapi4j.operation;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.domain.annotation.JsonApiRelationship;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.exception.OperationNotFoundException;
import pro.api4.jsonapi4j.response.CursorPageableResponse;
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
        TestAddToManyRelationshipOperation addToManyRelationshipOperation = new TestAddToManyRelationshipOperation();
        TestRemoveFromManyRelationshipOperation removeFromManyRelationshipOperation = new TestRemoveFromManyRelationshipOperation();
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
                .operation(addToManyRelationshipOperation)
                .operation(removeFromManyRelationshipOperation)
                .build();

        // then
        ResourceType fooResource = new ResourceType("foo");
        RelationshipName toOneRelationship = new RelationshipName("to1");
        RelationshipName toManyRelationship = new RelationshipName("to2");
        assertThat(sut.getAllOperations()).isNotNull().hasSize(11);
        assertThat(sut.getResourceTypesWithAnyOperationConfigured()).isNotNull().isEqualTo(Set.of(fooResource));
        assertThat(sut.getRelationshipNamesWithAnyOperationConfigured(fooResource)).isEqualTo(Set.of(toOneRelationship, toManyRelationship));
        assertThat(sut.getRelationshipNamesWithAnyOperationConfigured(new ResourceType("non-existing"))).isEmpty();

        assertThat(sut.isRelationshipOperationConfigured(fooResource, toOneRelationship, OperationType.READ_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toOneRelationship, OperationType.UPDATE_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.READ_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.UPDATE_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.ADD_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.REMOVE_FROM_MANY_RELATIONSHIP)).isTrue();
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
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.ADD_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.REMOVE_FROM_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, new RelationshipName("non-existing"), OperationType.READ_TO_MANY_RELATIONSHIP)).isFalse();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.READ_RESOURCE_BY_ID)).isFalse();

        assertThat(sut.getRegisteredReadResourceByIdOperation(fooResource, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(readByIdOperation);
        assertThat(sut.getRegisteredReadResourceByIdOperation(fooResource, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(readByIdOperation);
        assertThat(sut.getRegisteredReadResourceByIdOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredReadResourceByIdOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredReadMultipleResourcesOperation(fooResource, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(readMultipleResourcesOperation);
        assertThat(sut.getRegisteredReadMultipleResourcesOperation(fooResource, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(readMultipleResourcesOperation);
        assertThat(sut.getRegisteredReadMultipleResourcesOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredReadMultipleResourcesOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredCreateResourceOperation(fooResource, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(createResourceOperation);
        assertThat(sut.getRegisteredCreateResourceOperation(fooResource, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(createResourceOperation);
        assertThat(sut.getRegisteredCreateResourceOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredCreateResourceOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredUpdateResourceOperation(fooResource, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(updateResourceOperation);
        assertThat(sut.getRegisteredUpdateResourceOperation(fooResource, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(updateResourceOperation);
        assertThat(sut.getRegisteredUpdateResourceOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredUpdateResourceOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredDeleteResourceOperation(fooResource, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(deleteResourceOperation);
        assertThat(sut.getRegisteredDeleteResourceOperation(fooResource, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(deleteResourceOperation);
        assertThat(sut.getRegisteredDeleteResourceOperation(new ResourceType("non-existing"), false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredDeleteResourceOperation(new ResourceType("non-existing"), true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredReadToOneRelationshipOperation(fooResource, toOneRelationship, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(readToOneRelationshipOperation);
        assertThat(sut.getRegisteredReadToOneRelationshipOperation(fooResource, toOneRelationship, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(readToOneRelationshipOperation);
        assertThat(sut.getRegisteredReadToOneRelationshipOperation(new ResourceType("non-existing"), toOneRelationship,false)).isNull();
        assertThat(sut.getRegisteredReadToOneRelationshipOperation(fooResource, toManyRelationship,false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredReadToOneRelationshipOperation(new ResourceType("non-existing"), toOneRelationship,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getRegisteredReadToOneRelationshipOperation(fooResource, toManyRelationship,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredReadToManyRelationshipOperation(fooResource, toManyRelationship, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(readToManyRelationshipOperation);
        assertThat(sut.getRegisteredReadToManyRelationshipOperation(fooResource, toManyRelationship, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(readToManyRelationshipOperation);
        assertThat(sut.getRegisteredReadToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship,false)).isNull();
        assertThat(sut.getRegisteredReadToManyRelationshipOperation(fooResource, toOneRelationship,false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredReadToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getRegisteredReadToManyRelationshipOperation(fooResource, toOneRelationship,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredUpdateToOneRelationshipOperation(fooResource, toOneRelationship, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(updateToOneRelationshipOperation);
        assertThat(sut.getRegisteredUpdateToOneRelationshipOperation(fooResource, toOneRelationship, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(updateToOneRelationshipOperation);
        assertThat(sut.getRegisteredUpdateToOneRelationshipOperation(new ResourceType("non-existing"), toOneRelationship,false)).isNull();
        assertThat(sut.getRegisteredUpdateToOneRelationshipOperation(fooResource, toManyRelationship,false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredUpdateToOneRelationshipOperation(new ResourceType("non-existing"), toOneRelationship,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getRegisteredUpdateToOneRelationshipOperation(fooResource, toManyRelationship,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredUpdateToManyRelationshipOperation(fooResource, toManyRelationship, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(updateToManyRelationshipOperation);
        assertThat(sut.getRegisteredUpdateToManyRelationshipOperation(fooResource, toManyRelationship, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(updateToManyRelationshipOperation);
        assertThat(sut.getRegisteredUpdateToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship,false)).isNull();
        assertThat(sut.getRegisteredUpdateToManyRelationshipOperation(fooResource, toOneRelationship,false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredUpdateToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getRegisteredUpdateToManyRelationshipOperation(fooResource, toOneRelationship,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredAddToManyRelationshipOperation(fooResource, toManyRelationship, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(addToManyRelationshipOperation);
        assertThat(sut.getRegisteredAddToManyRelationshipOperation(fooResource, toManyRelationship, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(addToManyRelationshipOperation);
        assertThat(sut.getRegisteredAddToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship, false)).isNull();
        assertThat(sut.getRegisteredAddToManyRelationshipOperation(fooResource, toOneRelationship, false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredAddToManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship, true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getRegisteredAddToManyRelationshipOperation(fooResource, toOneRelationship, true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getRegisteredRemoveFromManyRelationshipOperation(fooResource, toManyRelationship, false)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(removeFromManyRelationshipOperation);
        assertThat(sut.getRegisteredRemoveFromManyRelationshipOperation(fooResource, toManyRelationship, true)).isNotNull().extracting(RegisteredOperation::getOperation).isEqualTo(removeFromManyRelationshipOperation);
        assertThat(sut.getRegisteredRemoveFromManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship, false)).isNull();
        assertThat(sut.getRegisteredRemoveFromManyRelationshipOperation(fooResource, toOneRelationship, false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredRemoveFromManyRelationshipOperation(new ResourceType("non-existing"), toManyRelationship, true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getRegisteredRemoveFromManyRelationshipOperation(fooResource, toOneRelationship, true)).isInstanceOf(OperationNotFoundException.class);
    }

    @Test
    public void registerToManyCompositeWithoutAddAndRemove_checkAddAndRemoveAreNotConfigured() {
        // given - when
        TestToManyRelationshipOperationsWithoutAddAndRemove operation = new TestToManyRelationshipOperationsWithoutAddAndRemove();
        OperationsRegistry sut = OperationsRegistry.builder(Collections.emptyList())
                .operation(operation)
                .build();

        // then
        ResourceType fooResource = new ResourceType("foo");
        RelationshipName toManyRelationship = new RelationshipName("to2");
        assertThat(sut.getAllOperations()).isNotNull().hasSize(2);
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.READ_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.UPDATE_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.ADD_TO_MANY_RELATIONSHIP)).isFalse();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.REMOVE_FROM_MANY_RELATIONSHIP)).isFalse();
        assertThat(sut.getRegisteredAddToManyRelationshipOperation(fooResource, toManyRelationship, false)).isNull();
        assertThat(sut.getRegisteredRemoveFromManyRelationshipOperation(fooResource, toManyRelationship, false)).isNull();
        assertThatThrownBy(() -> sut.getRegisteredAddToManyRelationshipOperation(fooResource, toManyRelationship, true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getRegisteredRemoveFromManyRelationshipOperation(fooResource, toManyRelationship, true)).isInstanceOf(OperationNotFoundException.class);
    }

    @Test
    public void registerToManyCompositeWithAddAndRemoveOverrides_checkAddAndRemoveAreConfigured() {
        // given - when
        TestToManyRelationshipOperationsWithAddAndRemove operation = new TestToManyRelationshipOperationsWithAddAndRemove();
        OperationsRegistry sut = OperationsRegistry.builder(Collections.emptyList())
                .operation(operation)
                .build();

        // then
        ResourceType fooResource = new ResourceType("foo");
        RelationshipName toManyRelationship = new RelationshipName("to2");
        assertThat(sut.getAllOperations()).isNotNull().hasSize(4);
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.ADD_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(fooResource, toManyRelationship, OperationType.REMOVE_FROM_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.getRegisteredAddToManyRelationshipOperation(fooResource, toManyRelationship, true)).isNotNull();
        assertThat(sut.getRegisteredRemoveFromManyRelationshipOperation(fooResource, toManyRelationship, true)).isNotNull();
    }
    
    @JsonApiResource(resourceType = "foo")
    private static class TestFooResource implements Resource<String> {

        @Override
        public String resolveResourceId(String dataSourceDto) {
            return dataSourceDto;
        }
    }
    
    @JsonApiRelationship(relationshipName = "to1", parentResource = TestFooResource.class)
    private static class TestToOneRelationship implements ToOneRelationship<String> {

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
    private static class TestToManyRelationship implements ToManyRelationship<String> {

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

    @JsonApiRelationshipOperation(relationship = TestToOneRelationship.class)
    private static class TestReadToOneRelationshipOperation implements ReadToOneRelationshipOperation<String, String> {

        @Override
        public String readOne(JsonApiRequest relationshipRequest) {
            return UUID.randomUUID().toString();
        }

    }

    @JsonApiRelationshipOperation(relationship = TestToManyRelationship.class)
    private static class TestReadToManyRelationshipOperation implements ReadToManyRelationshipOperation<String, String> {

        @Override
        public CursorPageableResponse<String> readMany(JsonApiRequest relationshipRequest) {
            return CursorPageableResponse.empty();
        }
    }

    @JsonApiRelationshipOperation(relationship = TestToOneRelationship.class)
    private static class TestUpdateToOneRelationshipOperation implements UpdateToOneRelationshipOperation {

        @Override
        public void update(JsonApiRequest request) {

        }
    }

    @JsonApiRelationshipOperation(relationship = TestToManyRelationship.class)
    private static class TestUpdateToManyRelationshipOperation implements UpdateToManyRelationshipOperation {

        @Override
        public void update(JsonApiRequest request) {

        }
    }

    @JsonApiRelationshipOperation(relationship = TestToManyRelationship.class)
    private static class TestToManyRelationshipOperationsWithoutAddAndRemove
            implements ToManyRelationshipOperations<String, String> {

        @Override
        public CursorPageableResponse<String> readMany(JsonApiRequest relationshipRequest) {
            return CursorPageableResponse.empty();
        }

        @Override
        public void update(JsonApiRequest request) {

        }
    }

    @JsonApiRelationshipOperation(relationship = TestToManyRelationship.class)
    private static class TestToManyRelationshipOperationsWithAddAndRemove
            implements ToManyRelationshipOperations<String, String> {

        @Override
        public CursorPageableResponse<String> readMany(JsonApiRequest relationshipRequest) {
            return CursorPageableResponse.empty();
        }

        @Override
        public void update(JsonApiRequest request) {

        }

        @Override
        public void add(JsonApiRequest request) {

        }

        @Override
        public void remove(JsonApiRequest request) {

        }
    }

    @JsonApiRelationshipOperation(relationship = TestToManyRelationship.class)
    private static class TestAddToManyRelationshipOperation implements AddToManyRelationshipOperation {

        @Override
        public void add(JsonApiRequest request) {

        }
    }

    @JsonApiRelationshipOperation(relationship = TestToManyRelationship.class)
    private static class TestRemoveFromManyRelationshipOperation implements RemoveFromManyRelationshipOperation {

        @Override
        public void remove(JsonApiRequest request) {

        }
    }

}
