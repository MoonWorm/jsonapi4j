package pro.api4.jsonapi4j.oas.customizer.util;

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
        return capitalize(resourceType.getType()) + "_Single_Resource_Doc";
    }

    public static String multipleResourcesDocSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "_Multiple_Resources_Doc";
    }

    public static String toManyRelationshipsDocSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "_To_Many_Relationships_Doc";
    }

    public static String toOneRelationshipDocSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "_To_One_Relationship_Doc";
    }

    public static String customToManyRelationshipDocSchemaName(ResourceType resourceType,
                                                               RelationshipName relationshipName) {
        return capitalize(resourceType.getType()) + "_" + capitalize(relationshipName.getName()) + "_To_Many_Relationships_Doc";
    }

    public static String customToOneRelationshipDocSchemaName(ResourceType resourceType,
                                                              RelationshipName relationshipName) {
        return capitalize(resourceType.getType()) + "_" + capitalize(relationshipName.getName()) + "_To_One_Relationship_Doc";

    }

    public static String attributesSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "_Attributes";
    }

    public static String relationshipsSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "_Relationships";
    }

    public static String resourceSchemaName(ResourceType resourceType) {
        return capitalize(resourceType.getType()) + "_Resource";
    }

    public static String customResourceIdentifierSchemaName(ResourceType resourceType,
                                                            RelationshipName relationshipName) {
        return capitalize(resourceType.getType()) + "_" + capitalize(relationshipName.getName()) + "_Resource_Identifier";

    }

    public static String customResourceIdentifierMetaSchemaName(ResourceType resourceType, RelationshipName relationshipName) {
        return capitalize(resourceType.getType()) + "_" + capitalize(relationshipName.getName()) + "_Resource_Identifier_Meta";
    }

    public static String errorsDocSchemaName() {
        return "Errors_Doc";
    }

}
