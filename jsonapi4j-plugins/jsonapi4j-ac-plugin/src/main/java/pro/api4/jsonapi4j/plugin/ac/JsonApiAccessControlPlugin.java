package pro.api4.jsonapi4j.plugin.ac;

import lombok.extern.slf4j.Slf4j;
import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.operation.CreateResourceOperation;
import pro.api4.jsonapi4j.operation.DeleteResourceOperation;
import pro.api4.jsonapi4j.operation.Operation;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.ReadToOneRelationshipOperation;
import pro.api4.jsonapi4j.operation.UpdateResourceOperation;
import pro.api4.jsonapi4j.operation.UpdateToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.UpdateToOneRelationshipOperation;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.MultipleResourcesVisitors;
import pro.api4.jsonapi4j.plugin.SingleResourceVisitors;
import pro.api4.jsonapi4j.plugin.ToManyRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.ToOneRelationshipVisitors;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.config.AcProperties;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResource;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.util.ReflectionUtils;

import static pro.api4.jsonapi4j.domain.Relationship.RESOLVE_RESOURCE_IDENTIFIER_META_METHOD_NAME;
import static pro.api4.jsonapi4j.domain.Resource.RESOLVE_ATTRIBUTES_METHOD_NAME;
import static pro.api4.jsonapi4j.domain.Resource.RESOLVE_RESOURCE_LINKS_METHOD_NAME;
import static pro.api4.jsonapi4j.domain.Resource.RESOLVE_RESOURCE_META_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.CreateResourceOperation.CREATE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.DeleteResourceOperation.DELETE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.READ_PAGE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.ReadResourceByIdOperation.READ_BY_ID_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation.READ_MANY_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.ReadToOneRelationshipOperation.READ_ONE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.UpdateResourceOperation.UPDATE_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.UpdateToManyRelationshipOperation.UPDATE_MANY_METHOD_NAME;
import static pro.api4.jsonapi4j.operation.UpdateToOneRelationshipOperation.UPDATE_ONE_METHOD_NAME;
import static pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel.merge;
import static pro.api4.jsonapi4j.util.ReflectionUtils.fetchAnnotationForMethod;

@Slf4j
public class JsonApiAccessControlPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiAccessControlPlugin.class.getSimpleName();

    private final AccessControlEvaluator accessControlEvaluator;
    private final AcProperties acProperties;

    public JsonApiAccessControlPlugin(AccessControlEvaluator accessControlEvaluator,
                                      AcProperties acProperties) {
        this.accessControlEvaluator = accessControlEvaluator;
        this.acProperties = acProperties;
    }

    private static AccessControlModel findOnTheOperationMethod(Class<?> operationType, String methodName) {
        return AccessControlModel.fromAnnotation(
                fetchAnnotationForMethod(
                        operationType,
                        methodName,
                        new Class<?>[]{JsonApiRequest.class},
                        AccessControl.class
                )
        );
    }

    @Override
    public String pluginName() {
        return NAME;
    }

    @Override
    public boolean enabled() {
        return acProperties.enabled();
    }

    @Override
    public int precedence() {
        return JsonApi4jPlugin.HIGH_PRECEDENCE;
    }

    @Override
    public Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        AccessControlModel classLevel = AccessControlModel.fromClassAnnotation(operation.getClass());
        if (ReadResourceByIdOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), READ_BY_ID_METHOD_NAME));
        }
        if (ReadMultipleResourcesOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), READ_PAGE_METHOD_NAME));
        }
        if (CreateResourceOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), CREATE_METHOD_NAME));
        }
        if (UpdateResourceOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), UPDATE_METHOD_NAME));
        }
        if (DeleteResourceOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), DELETE_METHOD_NAME));
        }
        if (ReadToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), READ_ONE_METHOD_NAME));
        }
        if (ReadToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), READ_MANY_METHOD_NAME));
        }
        if (UpdateToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), UPDATE_ONE_METHOD_NAME));
        }
        if (UpdateToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, findOnTheOperationMethod(operation.getClass(), UPDATE_MANY_METHOD_NAME));
        }
        return classLevel;
    }

    @Override
    public Object extractPluginInfoFromResource(Resource<?> resource) {
        AccessControlModel resourceClassLevel = AccessControlModel.fromClassAnnotation(resource.getClass());
        AccessControlModel resourceAttributesFieldLevel = AccessControlModel.fromAnnotation(
                ReflectionUtils.fetchAnnotationForMethod(
                        resource.getClass(),
                        RESOLVE_ATTRIBUTES_METHOD_NAME,
                        new Class<?>[]{Object.class},
                        AccessControl.class
                )
        );
        AccessControlModel resourceLinksFieldLevel = AccessControlModel.fromAnnotation(
                ReflectionUtils.fetchAnnotationForMethod(
                        resource.getClass(),
                        RESOLVE_RESOURCE_LINKS_METHOD_NAME,
                        new Class<?>[]{JsonApiRequest.class, Object.class},
                        AccessControl.class
                )
        );
        AccessControlModel resourceMetaFieldLevel = AccessControlModel.fromAnnotation(
                ReflectionUtils.fetchAnnotationForMethod(
                        resource.getClass(),
                        RESOLVE_RESOURCE_META_METHOD_NAME,
                        new Class<?>[]{JsonApiRequest.class, Object.class},
                        AccessControl.class
                )
        );
        return OutboundAccessControlForJsonApiResource.builder()
                .resourceClassLevel(resourceClassLevel)
                .resourceAttributesFieldLevel(resourceAttributesFieldLevel)
                .resourceLinksFieldLevel(resourceLinksFieldLevel)
                .resourceMetaFieldLevel(resourceMetaFieldLevel)
                .build();
    }

    @Override
    public Object extractPluginInfoFromRelationship(Relationship<?> relationship) {
        AccessControlModel resourceIdentifierClassLevel = AccessControlModel.fromClassAnnotation(
                relationship.getClass()
        );
        AccessControlModel resourceIdentifierMetaFieldLevel = AccessControlModel.fromAnnotation(
                ReflectionUtils.fetchAnnotationForMethod(
                        relationship.getClass(),
                        RESOLVE_RESOURCE_IDENTIFIER_META_METHOD_NAME,
                        new Class<?>[]{JsonApiRequest.class, Object.class},
                        AccessControl.class
                )
        );
        return OutboundAccessControlForJsonApiResourceIdentifier.builder()
                .resourceIdentifierClassLevel(resourceIdentifierClassLevel)
                .resourceIdentifierMetaFieldLevel(resourceIdentifierMetaFieldLevel)
                .build();
    }

    @Override
    public SingleResourceVisitors singleResourceVisitors() {
        return new AccessControlSingleResourceVisitors(accessControlEvaluator);
    }

    @Override
    public MultipleResourcesVisitors multipleResourcesVisitors() {
        return new AccessControlMultipleResourcesVisitors(accessControlEvaluator);
    }

    @Override
    public ToOneRelationshipVisitors toOneRelationshipVisitors() {
        return new AccessControlToOneRelationshipVisitors(accessControlEvaluator);
    }

    @Override
    public ToManyRelationshipVisitors toManyRelationshipVisitors() {
        return new AccessControlToManyRelationshipVisitors(accessControlEvaluator);
    }
}