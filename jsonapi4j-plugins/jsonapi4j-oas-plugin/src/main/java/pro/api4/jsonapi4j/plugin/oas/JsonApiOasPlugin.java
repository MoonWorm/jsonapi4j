package pro.api4.jsonapi4j.plugin.oas;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.operation.*;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasRelationshipInfo;
import pro.api4.jsonapi4j.plugin.oas.domain.annotation.OasResourceInfo;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasRelationshipInfoModel;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasResourceInfoModel;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.model.OasOperationInfoModel;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import static pro.api4.jsonapi4j.operation.CreateResourceOperation.CREATE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.DeleteResourceOperation.DELETE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.READ_PAGE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.ReadResourceByIdOperation.READ_BY_ID_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation.READ_MANY_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.ReadToOneRelationshipOperation.READ_ONE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.UpdateResourceOperation.UPDATE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.UpdateToManyRelationshipOperation.UPDATE_MANY_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.UpdateToOneRelationshipOperation.UPDATE_ONE_METHOD_NAME;
import static pro.api4.jsonapi4j.util.ReflectionUtils.fetchAnnotationForMethod;

public class JsonApiOasPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiOasPlugin.class.getSimpleName();

    private OasProperties oasProperties;

    public JsonApiOasPlugin(OasProperties oasProperties) {
        this.oasProperties = oasProperties;
    }

    private static OasOperationInfoModel findOnTheMethod(Class<?> operationType, String methodName) {
        return OasOperationInfoModel.fromAnnotation(
                fetchAnnotationForMethod(
                        operationType,
                        methodName,
                        new Class<?>[]{JsonApiRequest.class},
                        OasOperationInfo.class
                )
        );
    }

    private static Object getOrDefault(OasOperationInfoModel primary,
                                       OasOperationInfoModel secondary) {
        if (primary == null) {
            return secondary;
        }
        return primary;
    }

    @Override
    public String pluginName() {
        return NAME;
    }

    @Override
    public boolean enabled() {
        return oasProperties.enabled();
    }

    @Override
    public Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        OasOperationInfoModel classLevelModel = OasOperationInfoModel.fromAnnotation(
                ReflectionUtils.findAnnotationForClass(operation.getClass(), OasOperationInfo.class)
        );
        if (ReadResourceByIdOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), READ_BY_ID_METHOD_NAME),
                    classLevelModel
            );
        }
        if (ReadMultipleResourcesOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), READ_PAGE_METHOD_NAME),
                    classLevelModel
            );
        }
        if (CreateResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), CREATE_METHOD_NAME),
                    classLevelModel
            );
        }
        if (UpdateResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), UPDATE_METHOD_NAME),
                    classLevelModel
            );
        }
        if (DeleteResourceOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), DELETE_METHOD_NAME),
                    classLevelModel
            );
        }
        if (ReadToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), READ_ONE_METHOD_NAME),
                    classLevelModel
            );
        }
        if (ReadToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), READ_MANY_METHOD_NAME),
                    classLevelModel
            );
        }
        if (UpdateToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), UPDATE_ONE_METHOD_NAME),
                    classLevelModel
            );
        }
        if (UpdateToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return getOrDefault(
                    findOnTheMethod(operation.getClass(), UPDATE_MANY_METHOD_NAME),
                    classLevelModel
            );
        }
        return classLevelModel;
    }

    @Override
    public Object extractPluginInfoFromResource(Resource<?> resource) {
        return OasResourceInfoModel.fromAnnotation(
                ReflectionUtils.findAnnotationForClass(resource.getClass(), OasResourceInfo.class)
        );
    }

    @Override
    public Object extractPluginInfoFromRelationship(Relationship<?> relationship) {
        return OasRelationshipInfoModel.fromAnnotation(
                ReflectionUtils.findAnnotationForClass(relationship.getClass(), OasRelationshipInfo.class)
        );
    }

}
