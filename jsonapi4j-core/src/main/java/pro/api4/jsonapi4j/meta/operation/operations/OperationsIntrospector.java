package pro.api4.jsonapi4j.meta.operation.operations;

import pro.api4.jsonapi4j.meta.Ref;
import pro.api4.jsonapi4j.meta.domain.operations.OperationsResource.OperationDescriptorAttributes;

import java.util.List;
import java.util.Optional;

public interface OperationsIntrospector {

    List<OperationDescriptorAttributes> operations();

    Optional<OperationDescriptorAttributes> operationById(String id);

    List<Ref> operationRefs();

}
