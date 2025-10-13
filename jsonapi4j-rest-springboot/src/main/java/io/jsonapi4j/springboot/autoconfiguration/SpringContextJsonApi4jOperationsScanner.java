package io.jsonapi4j.springboot.autoconfiguration;

import io.jsonapi4j.operation.CreateResourceOperation;
import io.jsonapi4j.operation.DeleteResourceOperation;
import io.jsonapi4j.operation.ReadMultipleResourcesOperation;
import io.jsonapi4j.operation.ReadResourceByIdOperation;
import io.jsonapi4j.operation.ReadToManyRelationshipOperation;
import io.jsonapi4j.operation.ReadToOneRelationshipOperation;
import io.jsonapi4j.operation.UpdateResourceOperation;
import io.jsonapi4j.operation.UpdateToManyRelationshipOperation;
import io.jsonapi4j.operation.UpdateToOneRelationshipOperation;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.util.Set;

@Component
public class SpringContextJsonApi4jOperationsScanner extends SpringContextScanner {

    // resource operations
    private final ObjectProvider<Set<ReadResourceByIdOperation<?>>> readResourceByIdOperationsProvider;
    private final ObjectProvider<Set<ReadMultipleResourcesOperation<?>>> readMultipleResourcesOperationsProvider;
    private final ObjectProvider<Set<CreateResourceOperation<?>>> createResourceOperationsProvider;
    private final ObjectProvider<Set<UpdateResourceOperation>> updateResourceOperationsProvider;
    private final ObjectProvider<Set<DeleteResourceOperation>> deleteResourceOperationsProvider;

    // relationship operations
    private final ObjectProvider<Set<ReadToOneRelationshipOperation<?, ?>>> readToOneRelationshipOperationsProvider;
    private final ObjectProvider<Set<ReadToManyRelationshipOperation<?, ?>>> readToManyRelationshipOperationsProvider;
    private final ObjectProvider<Set<UpdateToOneRelationshipOperation>> updateToOneRelationshipOperationsProvider;
    private final ObjectProvider<Set<UpdateToManyRelationshipOperation>> updateToManyRelationshipOperationsProvider;

    public SpringContextJsonApi4jOperationsScanner(ObjectProvider<Set<ReadResourceByIdOperation<?>>> readResourceByIdOperationsProvider,
                                                   ObjectProvider<Set<ReadMultipleResourcesOperation<?>>> readMultipleResourcesOperationsProvider,
                                                   ObjectProvider<Set<CreateResourceOperation<?>>> createResourceOperationsProvider,
                                                   ObjectProvider<Set<UpdateResourceOperation>> updateResourceOperationsProvider,
                                                   ObjectProvider<Set<DeleteResourceOperation>> deleteResourceOperationsProvider,
                                                   ObjectProvider<Set<ReadToOneRelationshipOperation<?, ?>>> readToOneRelationshipOperationsProvider,
                                                   ObjectProvider<Set<ReadToManyRelationshipOperation<?, ?>>> readToManyRelationshipOperationsProvider,
                                                   ObjectProvider<Set<UpdateToOneRelationshipOperation>> updateToOneRelationshipOperationsProvider,
                                                   ObjectProvider<Set<UpdateToManyRelationshipOperation>> updateToManyRelationshipOperationsProvider) {
        this.readResourceByIdOperationsProvider = readResourceByIdOperationsProvider;
        this.readMultipleResourcesOperationsProvider = readMultipleResourcesOperationsProvider;
        this.createResourceOperationsProvider = createResourceOperationsProvider;
        this.updateResourceOperationsProvider = updateResourceOperationsProvider;
        this.deleteResourceOperationsProvider = deleteResourceOperationsProvider;
        this.readToOneRelationshipOperationsProvider = readToOneRelationshipOperationsProvider;
        this.readToManyRelationshipOperationsProvider = readToManyRelationshipOperationsProvider;
        this.updateToOneRelationshipOperationsProvider = updateToOneRelationshipOperationsProvider;
        this.updateToManyRelationshipOperationsProvider = updateToManyRelationshipOperationsProvider;
    }

    public Set<ReadResourceByIdOperation<?>> getReadResourceByIdOperations() {
        return get(readResourceByIdOperationsProvider);
    }

    public Set<ReadMultipleResourcesOperation<?>> getReadMultipleResourcesOperations() {
        return get(readMultipleResourcesOperationsProvider);
    }

    public Set<CreateResourceOperation<?>> getCreateResourceOperations() {
        return get(createResourceOperationsProvider);
    }

    public Set<UpdateResourceOperation> getUpdateResourceOperations() {
        return get(updateResourceOperationsProvider);
    }

    public Set<DeleteResourceOperation> getDeleteResourceOperations() {
        return get(deleteResourceOperationsProvider);
    }

    public Set<ReadToOneRelationshipOperation<?, ?>> getReadToOneRelationshipOperations() {
        return get(readToOneRelationshipOperationsProvider);
    }

    public Set<ReadToManyRelationshipOperation<?, ?>> getReadToManyRelationshipOperations() {
        return get(readToManyRelationshipOperationsProvider);
    }

    public Set<UpdateToOneRelationshipOperation> getUpdateToOneRelationshipOperations() {
        return get(updateToOneRelationshipOperationsProvider);
    }

    public Set<UpdateToManyRelationshipOperation> getUpdateToManyRelationshipOperations() {
        return get(updateToManyRelationshipOperationsProvider);
    }

}
