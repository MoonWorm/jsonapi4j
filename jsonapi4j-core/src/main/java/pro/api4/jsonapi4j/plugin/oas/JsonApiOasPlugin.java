package pro.api4.jsonapi4j.plugin.oas;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasResourceInfo;
import pro.api4.jsonapi4j.plugin.ac.JsonApiAccessControlPlugin;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.operation.*;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.function.Supplier;

import static pro.api4.jsonapi4j.plugin.utils.ReflectionUtils.fetchAnnotationForMethod;

public class JsonApiOasPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiAccessControlPlugin.class.getSimpleName();

    @Override
    public String pluginName() {
        return NAME;
    }

    @Override
    public Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        OasOperationInfo classLevel = operation.getClass().getAnnotation(OasOperationInfo.class);
        if (ReadResourceByIdOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "readById", OasOperationInfo.class),
                    classLevel
            );
        }
        if (ReadMultipleResourcesOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "readPage", OasOperationInfo.class),
                    classLevel
            );
        }
        if (CreateResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "create", OasOperationInfo.class),
                    classLevel
            );
        }
        if (UpdateResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "update", OasOperationInfo.class),
                    classLevel
            );
        }
        if (DeleteResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "delete", OasOperationInfo.class),
                    classLevel
            );
        }
        if (ReadToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "readOne", OasOperationInfo.class),
                    classLevel
            );
        }
        if (ReadToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "readMany", OasOperationInfo.class),
                    classLevel
            );
        }
        if (UpdateToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "update", OasOperationInfo.class),
                    classLevel
            );
        }
        if (UpdateToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> fetchAnnotationForMethod(operation.getClass(), "update", OasOperationInfo.class),
                    classLevel
            );
        }
        return classLevel;
    }

    @Override
    public Object extractPluginInfoFromResource(Resource<?> resource) {
        return resource.getClass().getAnnotation(OasResourceInfo.class);
    }

    @Override
    public Object extractPluginInfoFromRelationship(Relationship<?, ?> relationship) {
        return relationship.getClass().getAnnotation(OasRelationshipInfo.class);
    }

    private Object getOrDefault(Supplier<OasOperationInfo> primarySupplier,
                                OasOperationInfo secondary) {
        OasOperationInfo primary = primarySupplier.get();
        if (primary == null) {
            return secondary;
        }
        return primary;
    }

}
