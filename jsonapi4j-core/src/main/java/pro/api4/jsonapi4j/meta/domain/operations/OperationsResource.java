package pro.api4.jsonapi4j.meta.domain.operations;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.annotation.JsonApiResource;

import java.text.MessageFormat;

@JsonApiResource(resourceType = OperationsResource.OPERATIONS)
public class OperationsResource implements Resource<OperationsResource.OperationDescriptorAttributes> {

    public static final String OPERATIONS = "operations";

    public static String operationId(OperationDescriptorAttributes a) {
        return a.relationshipName() == null
                ? MessageFormat.format("{0}.{1}", a.resourceType(), a.operationType())
                : MessageFormat.format("{0}.{1}.{2}", a.resourceType(), a.relationshipName(), a.operationType());
    }

    @Override
    public String resolveResourceId(OperationDescriptorAttributes a) {
        return operationId(a);
    }

    @Override
    public Object resolveAttributes(OperationDescriptorAttributes a) {
        return a;
    }

    public record OperationDescriptorAttributes(String operationType,
                                                String httpMethod,
                                                String pathTemplate,
                                                String resourceType,
                                                String relationshipName,
                                                String className) {
    }
}
