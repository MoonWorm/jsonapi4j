package pro.api4.jsonapi4j.operation;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
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
        assertThat(sut.getAllOperations()).isNotNull().hasSize(9);
        assertThat(sut.getResourceTypesWithAnyOperationConfigured()).isNotNull().isEqualTo(Set.of(TestResourceTypes.FOO));
        assertThat(sut.getRelationshipNamesWithAnyOperationConfigured(TestResourceTypes.FOO)).isEqualTo(Set.of(TestRelationships.TO_ONE, TestRelationships.TO_MANY));
        assertThat(sut.getRelationshipNamesWithAnyOperationConfigured(TestResourceTypes.NON_EXISTENT)).isEmpty();

        assertThat(sut.isRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_ONE, OperationType.READ_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_ONE, OperationType.UPDATE_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_MANY, OperationType.READ_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_MANY, OperationType.UPDATE_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_MANY, OperationType.READ_RESOURCE_BY_ID)).isFalse();

        assertThat(sut.isAnyResourceOperationConfigured(TestResourceTypes.FOO)).isTrue();
        assertThat(sut.isAnyResourceOperationConfigured(TestResourceTypes.NON_EXISTENT)).isFalse();

        assertThat(sut.isAnyToOneRelationshipOperationConfigured(TestResourceTypes.FOO)).isTrue();
        assertThat(sut.isAnyToOneRelationshipOperationConfigured(TestResourceTypes.NON_EXISTENT)).isFalse();
        assertThat(sut.isAnyToOneRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_ONE)).isTrue();
        assertThat(sut.isAnyToOneRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.NON_EXISTENT)).isFalse();
        assertThat(sut.isToOneRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_ONE, OperationType.READ_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isToOneRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_ONE, OperationType.UPDATE_TO_ONE_RELATIONSHIP)).isTrue();
        assertThat(sut.isToOneRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.NON_EXISTENT, OperationType.READ_TO_ONE_RELATIONSHIP)).isFalse();
        assertThat(sut.isToOneRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_ONE, OperationType.READ_RESOURCE_BY_ID)).isFalse();

        assertThat(sut.isAnyToManyRelationshipOperationConfigured(TestResourceTypes.FOO)).isTrue();
        assertThat(sut.isAnyToManyRelationshipOperationConfigured(TestResourceTypes.NON_EXISTENT)).isFalse();
        assertThat(sut.isAnyToManyRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_MANY)).isTrue();
        assertThat(sut.isAnyToManyRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.NON_EXISTENT)).isFalse();
        assertThat(sut.isToManyRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_MANY, OperationType.READ_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_MANY, OperationType.UPDATE_TO_MANY_RELATIONSHIP)).isTrue();
        assertThat(sut.isToManyRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.NON_EXISTENT, OperationType.READ_TO_MANY_RELATIONSHIP)).isFalse();
        assertThat(sut.isToManyRelationshipOperationConfigured(TestResourceTypes.FOO, TestRelationships.TO_MANY, OperationType.READ_RESOURCE_BY_ID)).isFalse();

        assertThat(sut.getReadResourceByIdOperation(TestResourceTypes.FOO, false)).isNotNull().isEqualTo(readByIdOperation);
        assertThat(sut.getReadResourceByIdOperation(TestResourceTypes.FOO, true)).isNotNull().isEqualTo(readByIdOperation);
        assertThat(sut.getReadResourceByIdOperation(TestResourceTypes.NON_EXISTENT, false)).isNull();
        assertThatThrownBy(() -> sut.getReadResourceByIdOperation(TestResourceTypes.NON_EXISTENT, true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getReadMultipleResourcesOperation(TestResourceTypes.FOO, false)).isNotNull().isEqualTo(readMultipleResourcesOperation);
        assertThat(sut.getReadMultipleResourcesOperation(TestResourceTypes.FOO, true)).isNotNull().isEqualTo(readMultipleResourcesOperation);
        assertThat(sut.getReadMultipleResourcesOperation(TestResourceTypes.NON_EXISTENT, false)).isNull();
        assertThatThrownBy(() -> sut.getReadMultipleResourcesOperation(TestResourceTypes.NON_EXISTENT, true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getCreateResourceOperation(TestResourceTypes.FOO, false)).isNotNull().isEqualTo(createResourceOperation);
        assertThat(sut.getCreateResourceOperation(TestResourceTypes.FOO, true)).isNotNull().isEqualTo(createResourceOperation);
        assertThat(sut.getCreateResourceOperation(TestResourceTypes.NON_EXISTENT, false)).isNull();
        assertThatThrownBy(() -> sut.getCreateResourceOperation(TestResourceTypes.NON_EXISTENT, true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getUpdateResourceOperation(TestResourceTypes.FOO, false)).isNotNull().isEqualTo(updateResourceOperation);
        assertThat(sut.getUpdateResourceOperation(TestResourceTypes.FOO, true)).isNotNull().isEqualTo(updateResourceOperation);
        assertThat(sut.getUpdateResourceOperation(TestResourceTypes.NON_EXISTENT, false)).isNull();
        assertThatThrownBy(() -> sut.getUpdateResourceOperation(TestResourceTypes.NON_EXISTENT, true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getDeleteResourceOperation(TestResourceTypes.FOO, false)).isNotNull().isEqualTo(deleteResourceOperation);
        assertThat(sut.getDeleteResourceOperation(TestResourceTypes.FOO, true)).isNotNull().isEqualTo(deleteResourceOperation);
        assertThat(sut.getDeleteResourceOperation(TestResourceTypes.NON_EXISTENT, false)).isNull();
        assertThatThrownBy(() -> sut.getDeleteResourceOperation(TestResourceTypes.NON_EXISTENT, true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getReadToOneRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_ONE, false)).isNotNull().isEqualTo(readToOneRelationshipOperation);
        assertThat(sut.getReadToOneRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_ONE, true)).isNotNull().isEqualTo(readToOneRelationshipOperation);
        assertThat(sut.getReadToOneRelationshipOperation(TestResourceTypes.NON_EXISTENT, TestRelationships.TO_ONE,false)).isNull();
        assertThat(sut.getReadToOneRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_MANY,false)).isNull();
        assertThatThrownBy(() -> sut.getReadToOneRelationshipOperation(TestResourceTypes.NON_EXISTENT, TestRelationships.TO_ONE,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getReadToOneRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_MANY,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getReadToManyRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_MANY, false)).isNotNull().isEqualTo(readToManyRelationshipOperation);
        assertThat(sut.getReadToManyRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_MANY, true)).isNotNull().isEqualTo(readToManyRelationshipOperation);
        assertThat(sut.getReadToManyRelationshipOperation(TestResourceTypes.NON_EXISTENT, TestRelationships.TO_MANY,false)).isNull();
        assertThat(sut.getReadToManyRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_ONE,false)).isNull();
        assertThatThrownBy(() -> sut.getReadToManyRelationshipOperation(TestResourceTypes.NON_EXISTENT, TestRelationships.TO_MANY,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getReadToManyRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_ONE,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getUpdateToOneRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_ONE, false)).isNotNull().isEqualTo(updateToOneRelationshipOperation);
        assertThat(sut.getUpdateToOneRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_ONE, true)).isNotNull().isEqualTo(updateToOneRelationshipOperation);
        assertThat(sut.getUpdateToOneRelationshipOperation(TestResourceTypes.NON_EXISTENT, TestRelationships.TO_ONE,false)).isNull();
        assertThat(sut.getUpdateToOneRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_MANY,false)).isNull();
        assertThatThrownBy(() -> sut.getUpdateToOneRelationshipOperation(TestResourceTypes.NON_EXISTENT, TestRelationships.TO_ONE,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getUpdateToOneRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_MANY,true)).isInstanceOf(OperationNotFoundException.class);

        assertThat(sut.getUpdateToManyRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_MANY, false)).isNotNull().isEqualTo(updateToManyRelationshipOperation);
        assertThat(sut.getUpdateToManyRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_MANY, true)).isNotNull().isEqualTo(updateToManyRelationshipOperation);
        assertThat(sut.getUpdateToManyRelationshipOperation(TestResourceTypes.NON_EXISTENT, TestRelationships.TO_MANY,false)).isNull();
        assertThat(sut.getUpdateToManyRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_ONE,false)).isNull();
        assertThatThrownBy(() -> sut.getUpdateToManyRelationshipOperation(TestResourceTypes.NON_EXISTENT, TestRelationships.TO_MANY,true)).isInstanceOf(OperationNotFoundException.class);
        assertThatThrownBy(() -> sut.getUpdateToManyRelationshipOperation(TestResourceTypes.FOO, TestRelationships.TO_ONE,true)).isInstanceOf(OperationNotFoundException.class);
    }

    private static class TestReadByIdOperation implements ReadResourceByIdOperation<String> {

        @Override
        public String readById(JsonApiRequest request) {
            return UUID.randomUUID().toString();
        }

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

    }

    private static class TestReadMultipleResourcesOperation implements ReadMultipleResourcesOperation<String> {

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public CursorPageableResponse<String> readPage(JsonApiRequest request) {
            return CursorPageableResponse.empty();
        }

    }

    private static class TestCreateResourceOperation implements CreateResourceOperation {

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public Object create(JsonApiRequest request) {
            return UUID.randomUUID().toString();
        }
    }

    private static class TestUpdateResourceOperation implements UpdateResourceOperation {

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public void update(JsonApiRequest request) {

        }
    }

    private static class TestDeleteResourceOperation implements DeleteResourceOperation {

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public void delete(JsonApiRequest request) {

        }
    }

    private static class TestReadToOneRelationshipOperation implements ReadToOneRelationshipOperation<String, String> {

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public RelationshipName relationshipName() {
            return TestRelationships.TO_ONE;
        }

        @Override
        public String readOne(JsonApiRequest relationshipRequest) {
            return UUID.randomUUID().toString();
        }
    }

    private static class TestReadToManyRelationshipOperation implements ReadToManyRelationshipOperation<String, String> {

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public RelationshipName relationshipName() {
            return TestRelationships.TO_MANY;
        }

        @Override
        public CursorPageableResponse<String> readMany(JsonApiRequest relationshipRequest) {
            return CursorPageableResponse.empty();
        }
    }

    private static class TestUpdateToOneRelationshipOperation implements UpdateToOneRelationshipOperation {

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public RelationshipName relationshipName() {
            return TestRelationships.TO_ONE;
        }

        @Override
        public void update(JsonApiRequest request) {

        }
    }

    private static class TestUpdateToManyRelationshipOperation implements UpdateToManyRelationshipOperation {

        @Override
        public ResourceType resourceType() {
            return TestResourceTypes.FOO;
        }

        @Override
        public RelationshipName relationshipName() {
            return TestRelationships.TO_MANY;
        }

        @Override
        public void update(JsonApiRequest request) {

        }
    }

    private enum TestResourceTypes implements ResourceType {

        FOO("foo"),
        NON_EXISTENT("bar");

        private final String type;

        TestResourceTypes(String type) {
            this.type = type;
        }


        @Override
        public String getType() {
            return this.type;
        }
    }

    public enum TestRelationships implements RelationshipName {
        TO_ONE("to1"),
        TO_MANY("to2"),
        NON_EXISTENT("nonExistent");

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
