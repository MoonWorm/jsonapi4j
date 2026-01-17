package pro.api4.jsonapi4j.plugin.oas;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.plugin.oas.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.domain.plugin.oas.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfoModel;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasResourceInfoModel;
import pro.api4.jsonapi4j.operation.plugin.oas.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.operation.*;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfoModel;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;

import java.util.function.Supplier;

import static pro.api4.jsonapi4j.plugin.utils.ReflectionUtils.fetchAnnotationForMethod;

public class JsonApiOasPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiOasPlugin.class.getSimpleName();

    @Override
    public String pluginName() {
        return NAME;
    }

    @Override
    public Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        OasOperationInfoModel classLevelModel = OasOperationInfoModel.fromAnnotation(
                operation.getClass().getAnnotation(OasOperationInfo.class)
        );
        if (ReadResourceByIdOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "readById", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        if (ReadMultipleResourcesOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "readPage", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        if (CreateResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "create", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        if (UpdateResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "update", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        if (DeleteResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "delete", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        if (ReadToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "readOne", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        if (ReadToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "readMany", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        if (UpdateToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "update", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        if (UpdateToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    () -> OasOperationInfoModel.fromAnnotation(
                            fetchAnnotationForMethod(operation.getClass(), "update", OasOperationInfo.class)
                    ),
                    classLevelModel
            );
        }
        return classLevelModel;
    }

    @Override
    public Object extractPluginInfoFromResource(Resource<?> resource) {
        return OasResourceInfoModel.fromAnnotation(
                resource.getClass().getAnnotation(OasResourceInfo.class)
        );
    }

    @Override
    public Object extractPluginInfoFromRelationship(Relationship<?> relationship) {
        return OasRelationshipInfoModel.fromAnnotation(
                relationship.getClass().getAnnotation(OasRelationshipInfo.class)
        );
    }

    private Object getOrDefault(Supplier<OasOperationInfoModel> primarySupplier,
                                OasOperationInfoModel secondary) {
        OasOperationInfoModel primary = primarySupplier.get();
        if (primary == null) {
            return secondary;
        }
        return primary;
    }

}
