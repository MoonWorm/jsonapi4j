package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.OperationType;

import static pro.api4.jsonapi4j.operation.OperationType.SubType.RESOURCE;
import static pro.api4.jsonapi4j.operation.OperationType.SubType.TO_MANY_RELATIONSHIP;
import static pro.api4.jsonapi4j.operation.OperationType.SubType.TO_ONE_RELATIONSHIP;
import static org.apache.commons.lang3.StringUtils.capitalize;

public final class OasSchemaNamesUtil {

    private OasSchemaNamesUtil() {

    }

    public static String happyPathResponseDocSchemaName(ResourceType resourceType,
                                                        OperationType operationType) {
        if (operationType.getSubType() == RESOURCE) {
            switch (OasOperationInfoUtil.resolveOperationResponseType(operationType)) {
                case SINGLE_DATA -> {
                    return singleResourceDocSchemaName(resourceType);
                }
                case MULTI_DATA -> {
                    return multipleResourcesDocSchemaName(resourceType);
                }
            }
        } else if (operationType.getSubType() == TO_MANY_RELATIONSHIP
                || operationType.getSubType() == TO_ONE_RELATIONSHIP) {
            switch (OasOperationInfoUtil.resolveOperationResponseType(operationType)) {
                case SINGLE_DATA -> {
                    return toOneRelationshipDocSchemaName(resourceType);
                }
                case MULTI_DATA -> {
                    return toManyRelationshipsDocSchemaName(resourceType);
                }
            }
        }
        return null;
    }

    public static String singleResourceDocSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "SingleResourceDoc";
    }

    public static String multipleResourcesDocSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "MultipleResourcesDoc";
    }

    public static String toManyRelationshipsDocSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "ToManyRelationshipsDoc";
    }

    public static String toOneRelationshipDocSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "ToOneRelationshipDoc";
    }

    public static String customToManyRelationshipDocSchemaName(ResourceType resourceType,
                                                               RelationshipName relationshipName) {
        return capitalize(resourceType.getType()) + capitalize(relationshipName.getName()) + "ToManyRelationshipsDoc";
    }

    public static String customToOneRelationshipDocSchemaName(ResourceType resourceType,
                                                              RelationshipName relationshipName) {
        return capitalize(resourceType.getType()) + capitalize(relationshipName.getName()) + "ToOneRelationshipDoc";

    }

    public static String attributesSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "Attributes";
    }

    public static String relationshipsSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "Relationships";
    }

    public static String resourceSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "Resource";
    }

    public static String customResourceIdentifierSchemaName(ResourceType resourceType,
                                                            RelationshipName relationshipName) {
        return capitalize(resourceType.getType()) + capitalize(relationshipName.getName()) + "ResourceIdentifier";

    }

    public static String customResourceIdentifierMetaSchemaName(ResourceType resourceType, RelationshipName relationshipName) {
        return capitalize(resourceType.getType()) + capitalize(relationshipName.getName()) + "ResourceIdentifierMeta";
    }

    public static String errorsDocSchemaName() {
        return "ErrorsDoc";
    }

}
