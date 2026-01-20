package pro.api4.jsonapi4j.plugin.ac;

import pro.api4.jsonapi4j.domain.Relationship;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.operation.*;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResource;
import pro.api4.jsonapi4j.plugin.ac.model.outbound.OutboundAccessControlForJsonApiResourceIdentifier;
import pro.api4.jsonapi4j.plugin.utils.ReflectionUtils;

import java.util.Map;

import static pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel.merge;

public class JsonApiAccessControlPlugin implements JsonApi4jPlugin {

    public static final String NAME = JsonApiAccessControlPlugin.class.getSimpleName();

    @Override
    public String pluginName() {
        return NAME;
    }

    @Override
    public Object extractPluginInfoFromOperation(Operation operation, Class<?> operationClass) {
        Map<String, AccessControlModel> methodsAnnotations
                = AccessControlModel.fromFieldsAnnotations(operation.getClass());
        AccessControlModel classLevel = AccessControlModel.fromClassAnnotation(operation.getClass());
        if (ReadResourceByIdOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("readById"));
        }
        if (ReadMultipleResourcesOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("readPage"));
        }
        if (CreateResourceOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("create"));
        }
        if (UpdateResourceOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("update"));
        }
        if (DeleteResourceOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("delete"));
        }
        if (ReadToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("readOne"));
        }
        if (ReadToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("readMany"));
        }
        if (UpdateToOneRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("update"));
        }
        if (UpdateToManyRelationshipOperation.class.isAssignableFrom(operationClass)) {
            return merge(classLevel, methodsAnnotations.get("update"));
        }
        return classLevel;
    }

    @Override
    public Object extractPluginInfoFromResource(Resource<?> resource) {
        AccessControlModel resourceClassLevel = AccessControlModel.fromClassAnnotation(resource.getClass());
        AccessControlModel resourceAttributesFieldLevel = AccessControlModel.fromAnnotation(
                ReflectionUtils.fetchAnnotationForMethod(
                        resource.getClass(),
                        "resolveAttributes",
                        AccessControl.class
                )
        );
        AccessControlModel resourceLinksFieldLevel = AccessControlModel.fromAnnotation(
                ReflectionUtils.fetchAnnotationForMethod(
                        resource.getClass(),
                        "resolveResourceLinks",
                        AccessControl.class
                )
        );
        AccessControlModel resourceMetaFieldLevel = AccessControlModel.fromAnnotation(
                ReflectionUtils.fetchAnnotationForMethod(
                        resource.getClass(),
                        "resolveResourceMeta",
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
                        "resolveResourceIdentifierMeta",
                        AccessControl.class
                )
        );
        return OutboundAccessControlForJsonApiResourceIdentifier.builder()
                .resourceIdentifierClassLevel(resourceIdentifierClassLevel)
                .resourceIdentifierMetaFieldLevel(resourceIdentifierMetaFieldLevel)
                .build();
    }

}
