package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.http.HttpStatusCodes;
import pro.api4.jsonapi4j.operation.OperationType;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.operation.RegisteredOperation;

import java.util.Set;

import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_400_BAD_REQUEST;
import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_404_RESOURCE_NOT_FOUND;
import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_405_METHOD_NOT_SUPPORTED;
import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_406_NOT_ACCEPTABLE;
import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_415_UNSUPPORTED_MEDIA_TYPE;
import static pro.api4.jsonapi4j.http.HttpStatusCodes.SC_500_INTERNAL_SERVER_ERROR;
import static pro.api4.jsonapi4j.operation.OperationType.Method.GET;
import static pro.api4.jsonapi4j.operation.OperationType.READ_MULTIPLE_RESOURCES;
import static pro.api4.jsonapi4j.operation.OperationType.READ_TO_MANY_RELATIONSHIP;
import static org.apache.commons.lang3.StringUtils.capitalize;
import static org.apache.commons.lang3.StringUtils.uncapitalize;


public final class OasOperationInfoUtil {

    private OasOperationInfoUtil() {

    }

    public static Info resolveOperationOasInfo(RegisteredOperation.OperationMeta operationMeta,
                                               String customResourceNameSingle,
                                               String customResourceNamePlural) {
        OperationType operationType = operationMeta.getOperationType();
        ResourceType resourceType = operationMeta.getResourceType();
        RelationshipName relationshipName = operationMeta.getRelationshipName();

        String urlCompatibleUniqueName = resolveOperationUrlCompatibleName(
                operationType,
                resourceType,
                relationshipName,
                customResourceNameSingle,
                customResourceNamePlural
        );
        String operationTag = resolveOperationTag(resourceType);
        String operationSummary = resolveOperationSummary(
                operationType,
                resourceType,
                relationshipName,
                customResourceNameSingle,
                customResourceNamePlural
        );
        String operationDescription = resolveOperationDescription(
                operationType,
                resourceType,
                relationshipName,
                customResourceNameSingle
        );
        boolean isIncludesSupported = isIncludesSupported(operationType);
        boolean isPaginationSupported = isPaginationSupported(operationType);
        ResponseType responseType = resolveOperationResponseType(operationType);
        Set<HttpStatusCodes> supportedHttpErrorCodes = resolveSupportedHttpErrorCodes(operationType);
        return new Info(
                operationType,
                resourceType,
                relationshipName,
                urlCompatibleUniqueName,
                operationTag,
                operationSummary,
                operationDescription,
                isIncludesSupported,
                isPaginationSupported,
                responseType,
                supportedHttpErrorCodes
        );
    }

    public static String resolveOperationTag(ResourceType resourceType) {
        return capitalize(resourceType.getType());
    }

    private static String resolveOperationDescription(OperationType operationType,
                                                      ResourceType resourceType,
                                                      RelationshipName relationshipName,
                                                      String customResourceNameSingle) {
        if (OperationType.READ_RESOURCE_BY_ID == operationType) {
            return "Retrieves " + resourceNameSingular(resourceType, customResourceNameSingle) + " details by resource id.";
        } else if (OperationType.READ_MULTIPLE_RESOURCES == operationType) {
            return "Retrieves all " + resourceNameSingular(resourceType, customResourceNameSingle) + " details with available filters (if applicable).";
        } else if (OperationType.CREATE_RESOURCE == operationType) {
            return "Creates a new instance of " + resourceNameSingular(resourceType, customResourceNameSingle) + "'s resource with relationships (if applicable).";
        } else if (OperationType.UPDATE_RESOURCE == operationType) {
            return "Updates the existing instance of " + resourceNameSingular(resourceType, customResourceNameSingle) + "'s resource.";
        } else if (OperationType.DELETE_RESOURCE == operationType) {
            return "Deletes the existing instance of " + resourceNameSingular(resourceType, customResourceNameSingle) + "'s resource.";
        } else if (OperationType.READ_TO_ONE_RELATIONSHIP == operationType) {
            return "Retrieves " + relationshipName.getName() + " to-one relationship details of the related " + resourceNameSingular(resourceType, customResourceNameSingle) + " resource.";
        } else if (OperationType.UPDATE_TO_ONE_RELATIONSHIP == operationType) {
            return "Manages " + relationshipName.getName() + " to-one relationship details of the related " + resourceNameSingular(resourceType, customResourceNameSingle) + " resource. Use this operation if you need to create, update or delete relationship's linkage.";
        } else if (OperationType.READ_TO_MANY_RELATIONSHIP == operationType) {
            return "Retrieves " + relationshipName.getName() + " to-many relationship details of the related " + resourceNameSingular(resourceType, customResourceNameSingle) + " resource.";
        } else if (OperationType.UPDATE_TO_MANY_RELATIONSHIP == operationType) {
            return "Manages " + relationshipName.getName() + " to-many relationship details of the related " + resourceNameSingular(resourceType, customResourceNameSingle) + " resource. Use this operation if you need to create, update or delete relationship's linkage.";
        } else if (OperationType.ADD_TO_MANY_RELATIONSHIP == operationType) {
            return "Adds one or many linkage object(s) to " + relationshipName.getName() + " to-many relationship details of the related " + resourceNameSingular(resourceType, customResourceNameSingle) + " resource.";
        } else if (OperationType.REMOVE_FROM_MANY_RELATIONSHIP == operationType) {
            return "Removes one or many linkage object(s) from " + relationshipName.getName() + " to-many relationship details of the related " + resourceNameSingular(resourceType, customResourceNameSingle) + " resource.";
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + operationType);
        }
    }

    private static String resolveOperationUrlCompatibleName(OperationType operationType,
                                                            ResourceType resourceType,
                                                            RelationshipName relationshipName,
                                                            String customResourceNameSingle,
                                                            String customResourceNamePlural) {
        if (OperationType.READ_RESOURCE_BY_ID == operationType) {
            return "get-single-" + resourceNameSingular(resourceType, customResourceNameSingle);
        } else if (OperationType.READ_MULTIPLE_RESOURCES == operationType) {
            return "get-all-" + resourceNamePlural(resourceType, customResourceNamePlural).toLowerCase();
        } else if (OperationType.CREATE_RESOURCE == operationType) {
            return "create-single-" + resourceNameSingular(resourceType, customResourceNameSingle);
        } else if (OperationType.UPDATE_RESOURCE == operationType) {
            return "update-single-" + resourceNameSingular(resourceType, customResourceNameSingle);
        } else if (OperationType.DELETE_RESOURCE == operationType) {
            return "delete-single-" + resourceNameSingular(resourceType, customResourceNameSingle);
        } else if (OperationType.READ_TO_ONE_RELATIONSHIP == operationType) {
            return "get-" + resourceNameSingular(resourceType, customResourceNameSingle) + "-" + relationshipName.getName().toLowerCase() + "-relationship";
        } else if (OperationType.UPDATE_TO_ONE_RELATIONSHIP == operationType) {
            return "update-" + resourceNameSingular(resourceType, customResourceNameSingle) + "-" + relationshipName.getName().toLowerCase() + "-relationship";
        } else if (OperationType.READ_TO_MANY_RELATIONSHIP == operationType) {
            return "get-" + resourceNameSingular(resourceType, customResourceNameSingle) + "-" + relationshipName.getName().toLowerCase() + "-relationship";
        } else if (OperationType.UPDATE_TO_MANY_RELATIONSHIP == operationType) {
            return "update-" + resourceNameSingular(resourceType, customResourceNameSingle) + "-" + relationshipName.getName().toLowerCase() + "-relationship";
        } else if (OperationType.ADD_TO_MANY_RELATIONSHIP == operationType) {
            return "add-" + resourceNameSingular(resourceType, customResourceNameSingle) + "-" + relationshipName.getName().toLowerCase() + "-relationship";
        } else if (OperationType.REMOVE_FROM_MANY_RELATIONSHIP == operationType) {
            return "remove-" + resourceNameSingular(resourceType, customResourceNameSingle) + "-" + relationshipName.getName().toLowerCase() + "-relationship";
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + operationType);
        }
    }

    private static String resolveOperationSummary(OperationType operationType,
                                                  ResourceType resourceType,
                                                  RelationshipName relationshipName,
                                                  String customResourceNameSingle,
                                                  String customResourceNamePlural) {
        if (OperationType.READ_RESOURCE_BY_ID == operationType) {
            return "Get single " + resourceNameSingular(resourceType, customResourceNameSingle);
        } else if (OperationType.READ_MULTIPLE_RESOURCES == operationType) {
            return "Get all " + resourceNamePlural(resourceType, customResourceNamePlural);
        } else if (OperationType.CREATE_RESOURCE == operationType) {
            return "Create single " + resourceNameSingular(resourceType, customResourceNameSingle);
        } else if (OperationType.UPDATE_RESOURCE == operationType) {
            return "Update single " + resourceNameSingular(resourceType, customResourceNameSingle);
        } else if (OperationType.DELETE_RESOURCE == operationType) {
            return "Delete single " + resourceNameSingular(resourceType, customResourceNameSingle);
        } else if (OperationType.READ_TO_ONE_RELATIONSHIP == operationType) {
            return "Relationship: " + relationshipName.getName() + "(read)";
        } else if (OperationType.UPDATE_TO_ONE_RELATIONSHIP == operationType) {
            return "Relationship: " + relationshipName.getName() + " (create/update/delete)";
        } else if (OperationType.READ_TO_MANY_RELATIONSHIP == operationType) {
            return "Relationship: " + relationshipName.getName();
        } else if (OperationType.UPDATE_TO_MANY_RELATIONSHIP == operationType) {
            return "Relationship: " + relationshipName.getName() + " (create/update/delete)";
        } else if (OperationType.ADD_TO_MANY_RELATIONSHIP == operationType) {
            return "Relationship: " + relationshipName.getName() + " (add)";
        } else if (OperationType.REMOVE_FROM_MANY_RELATIONSHIP == operationType) {
            return "Relationship: " + relationshipName.getName() + " (remove)";
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + operationType);
        }
    }

    public static String resolveResourceOperationPath(String rootPath,
                                                      OperationType operationType,
                                                      ResourceType resourceType) {
        String effectiveRootPath;
        if (StringUtils.isNotBlank(rootPath) && rootPath.trim().equals("/")) {
            effectiveRootPath = "";
        } else {
            effectiveRootPath = rootPath;
        }
        if (operationType == OperationType.READ_RESOURCE_BY_ID) {
            return String.format("%s/%s/{id}", effectiveRootPath, resourceType.getType());
        } else if (operationType == OperationType.READ_MULTIPLE_RESOURCES) {
            return String.format("%s/%s", effectiveRootPath, resourceType.getType());
        } else if (operationType == OperationType.CREATE_RESOURCE) {
            return String.format("%s/%s", effectiveRootPath, resourceType.getType());
        } else if (operationType == OperationType.UPDATE_RESOURCE) {
            return String.format("%s/%s/{id}", effectiveRootPath, resourceType.getType());
        } else if (operationType == OperationType.DELETE_RESOURCE) {
            return String.format("%s/%s/{id}", effectiveRootPath, resourceType.getType());
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + operationType);
        }
    }

    public static String resolveRelationshipOperationPath(String rootPath,
                                                          OperationType operationType,
                                                          ResourceType resourceType,
                                                          RelationshipName relationshipName) {
        String effectiveRootPath;
        if (StringUtils.isNotBlank(rootPath) && rootPath.trim().equals("/")) {
            effectiveRootPath = "";
        } else {
            effectiveRootPath = rootPath;
        }
        if (operationType == OperationType.READ_TO_ONE_RELATIONSHIP) {
            return String.format("%s/%s/{id}/relationships/%s", effectiveRootPath, resourceType.getType(), relationshipName.getName());
        } else if (operationType == OperationType.UPDATE_TO_ONE_RELATIONSHIP) {
            return String.format("%s/%s/{id}/relationships/%s", effectiveRootPath, resourceType.getType(), relationshipName.getName());
        } else if (operationType == OperationType.READ_TO_MANY_RELATIONSHIP) {
            return String.format("%s/%s/{id}/relationships/%s", effectiveRootPath, resourceType.getType(), relationshipName.getName());
        } else if (operationType == OperationType.UPDATE_TO_MANY_RELATIONSHIP) {
            return String.format("%s/%s/{id}/relationships/%s", effectiveRootPath, resourceType.getType(), relationshipName.getName());
        } else if (operationType == OperationType.ADD_TO_MANY_RELATIONSHIP) {
            return String.format("%s/%s/{id}/relationships/%s", effectiveRootPath, resourceType.getType(), relationshipName.getName());
        } else if (operationType == OperationType.REMOVE_FROM_MANY_RELATIONSHIP) {
            return String.format("%s/%s/{id}/relationships/%s", effectiveRootPath, resourceType.getType(), relationshipName.getName());
        } else {
            throw new IllegalArgumentException("Unsupported operation type: " + operationType);
        }
    }

    private static String resourceNameSingular(ResourceType resourceType,
                                               String customResourceNameSingle) {
        return StringUtils.isBlank(customResourceNameSingle)
                ? OasOperationInfoUtil.removeLastCharIfPlural(resourceType)
                : customResourceNameSingle;
    }

    private static String resourceNamePlural(ResourceType resourceType,
                                             String customResourceNamePlural) {
        return StringUtils.isBlank(customResourceNamePlural)
                ? uncapitalize(resourceType.getType())
                : customResourceNamePlural;
    }

    private static String removeLastCharIfPlural(ResourceType resourceType) {
        String str = resourceType.getType();
        if (StringUtils.isBlank(str)) {
            return str;
        }
        if (str.charAt(str.length() - 1) == 's') {
            return str.substring(0, str.length() - 1);
        }
        return uncapitalize(str);
    }

    public static ResponseType resolveOperationResponseType(OperationType operationType) {
        return switch (operationType) {
            case UPDATE_RESOURCE,
                 DELETE_RESOURCE,
                 UPDATE_TO_ONE_RELATIONSHIP,
                 UPDATE_TO_MANY_RELATIONSHIP,
                 ADD_TO_MANY_RELATIONSHIP,
                 REMOVE_FROM_MANY_RELATIONSHIP ->
                    ResponseType.VOID;
            case READ_RESOURCE_BY_ID, CREATE_RESOURCE, READ_TO_ONE_RELATIONSHIP -> ResponseType.SINGLE_DATA;
            case READ_MULTIPLE_RESOURCES, READ_TO_MANY_RELATIONSHIP -> ResponseType.MULTI_DATA;
        };
    }

    private static boolean isPaginationSupported(OperationType operationType) {
        return operationType == READ_MULTIPLE_RESOURCES
                || operationType == READ_TO_MANY_RELATIONSHIP;
    }

    private static boolean isIncludesSupported(OperationType operationType) {
        return operationType.getMethod() == GET;
    }

    private static Set<HttpStatusCodes> resolveSupportedHttpErrorCodes(OperationType operationType) {
        switch (operationType) {
            case READ_RESOURCE_BY_ID,
                 READ_MULTIPLE_RESOURCES,
                 DELETE_RESOURCE,
                 READ_TO_ONE_RELATIONSHIP,
                 READ_TO_MANY_RELATIONSHIP:
                return Set.of(
                        SC_400_BAD_REQUEST,
                        SC_404_RESOURCE_NOT_FOUND,
                        SC_405_METHOD_NOT_SUPPORTED,
                        SC_406_NOT_ACCEPTABLE,
                        SC_500_INTERNAL_SERVER_ERROR
                );
            case CREATE_RESOURCE,
                 UPDATE_RESOURCE,
                 UPDATE_TO_ONE_RELATIONSHIP,
                 UPDATE_TO_MANY_RELATIONSHIP,
                 ADD_TO_MANY_RELATIONSHIP,
                 REMOVE_FROM_MANY_RELATIONSHIP:
                return Set.of(
                        SC_400_BAD_REQUEST,
                        SC_404_RESOURCE_NOT_FOUND,
                        SC_405_METHOD_NOT_SUPPORTED,
                        SC_406_NOT_ACCEPTABLE,
                        SC_415_UNSUPPORTED_MEDIA_TYPE,
                        SC_500_INTERNAL_SERVER_ERROR
                );
            default:
                throw new IllegalArgumentException("Unsupported operation type: " + operationType);
        }
    }

    public enum ResponseType {
        SINGLE_DATA, MULTI_DATA, VOID
    }

    @Data
    public static class Info {
        private final OperationType operationType;
        private final ResourceType resourceType;
        private final RelationshipName relationshipName;
        private final String urlCompatibleUniqueName;
        private final String operationTag;
        private final String summary;
        private final String description;
        private final boolean isIncludesSupported;
        private final boolean isPaginationSupported;
        private final ResponseType responseType;
        private final Set<HttpStatusCodes> supportedHttpErrorCodes;
    }
}
