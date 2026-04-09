package pro.api4.jsonapi4j.operation.validation;

import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;

import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;

public class JsonApi4jDefaultValidator {

    public static final int MAX_ELEMENTS_IN_FILTER_PARAM = 20;
    public static final int RESOURCE_ID_MAX_LENGTH = 64;

    public void validateNonNull(Object object, String parameter) {
        if (object == null) {
            throw new JsonApi4jConstraintViolationException("value can't be null", parameter);
        }
    }

    public void validateNonBlank(String value, String parameter) {
        if (StringUtils.isBlank(value)) {
            throw new JsonApi4jConstraintViolationException(String.format("'%s' can't be blank", parameter), parameter);
        }
    }

    public void validateFilterByIds(List<String> resourceIds) {
        if (resourceIds != null) {
            if (resourceIds.size() > MAX_ELEMENTS_IN_FILTER_PARAM) {
                throw new JsonApi4jConstraintViolationException(
                        "max elements number exceeded: " + MAX_ELEMENTS_IN_FILTER_PARAM,
                        FiltersAwareRequest.getFilterParam(ID_FILTER_NAME)
                );
            }
            for (String resourceId : resourceIds) {
                validateResourceId(resourceId);
            }
        }
    }

    public void validateResourceId(String resourceId, String parameterName) {
        if (StringUtils.isBlank(resourceId)) {
            throw new JsonApi4jConstraintViolationException(
                    "resource id can't be blank",
                    parameterName
            );
        }
        if (resourceId.length() > RESOURCE_ID_MAX_LENGTH) {
            throw new JsonApi4jConstraintViolationException(
                    "resource id length can't be more than " + RESOURCE_ID_MAX_LENGTH,
                    parameterName
            );
        }
    }

    public void validateResourceId(String resourceId) {
        validateResourceId(resourceId, "resourceId");
    }

    public void validateResourceTypeAnyOf(String resourceTypeToEvaluate,
                                          Set<String> validResourceTypes) {
        for (String validResourceType : validResourceTypes) {
            if (validResourceType.equalsIgnoreCase(resourceTypeToEvaluate)) {
                return;
            }
        }
        throw new JsonApi4jConstraintViolationException(
                String.format("resource type '%s' not supported, available resource types: [%s]", resourceTypeToEvaluate, String.join(", ", validResourceTypes)),
                "resourceType"
        );
    }

    public void validateSingleResourceDoc(SingleResourceDoc<?> singleResourceDoc) {
        if (singleResourceDoc.getData() == null) {
            throw new JsonApi4jConstraintViolationException("'data' is null", "data");
        }
        if (singleResourceDoc.getData().getId() != null) {
            validateResourceId(singleResourceDoc.getData().getId());
        }
        if (singleResourceDoc.getData().getType() != null) {
            validateNonBlank(singleResourceDoc.getData().getType(), "type");
        }
    }

    public void validateToOneRelationshipDoc(ToOneRelationshipDoc toOneRelationshipDoc) {
        if (toOneRelationshipDoc.getData() != null) {
            if (toOneRelationshipDoc.getData().getId() != null) {
                validateResourceId(toOneRelationshipDoc.getData().getId());
            }
            if (toOneRelationshipDoc.getData().getType() != null) {
                validateNonBlank(toOneRelationshipDoc.getData().getType(), "type");
            }
        }
    }

    public void validateToOneRelationshipDoc(ToOneRelationshipDoc toOneRelationshipDoc,
                                             Consumer<String> resourceIdValidator,
                                             Consumer<String> resourceTypeValidator) {
        validateToOneRelationshipDoc(toOneRelationshipDoc);
        if (toOneRelationshipDoc.getData() != null) {
            if (toOneRelationshipDoc.getData().getId() != null) {
                resourceIdValidator.accept(toOneRelationshipDoc.getData().getId());
            }
            if (toOneRelationshipDoc.getData().getType() != null) {
                resourceTypeValidator.accept(toOneRelationshipDoc.getData().getType());
            }
        }
    }

    public void validateToOneRelationshipDoc(ToOneRelationshipDoc toOneRelationshipDoc,
                                             Consumer<String> resourceIdValidator,
                                             Consumer<String> resourceTypeValidator,
                                             Consumer<Object> metaValidator) {
        validateToOneRelationshipDoc(toOneRelationshipDoc, resourceIdValidator, resourceTypeValidator);
        if (toOneRelationshipDoc.getData() != null) {
            if (toOneRelationshipDoc.getData().getMeta() != null) {
                metaValidator.accept(toOneRelationshipDoc.getData().getMeta());
            }
        }
    }

    public void validateToManyRelationshipDoc(ToManyRelationshipsDoc toManyRelationshipDoc) {
        if (toManyRelationshipDoc.getData() == null) {
            throw new JsonApi4jConstraintViolationException("'data' is null", "data");
        }
        toManyRelationshipDoc.getData().forEach(ri -> {
            validateResourceId(ri.getId());
            validateNonBlank(ri.getType(), "type");
        });
    }

    public void validateToManyRelationshipDoc(ToManyRelationshipsDoc toManyRelationshipDoc,
                                              Consumer<String> resourceIdValidator,
                                              Consumer<String> resourceTypeValidator) {
        validateToManyRelationshipDoc(toManyRelationshipDoc);
        toManyRelationshipDoc.getData().forEach(ri -> {
            resourceIdValidator.accept(ri.getId());
            resourceTypeValidator.accept(ri.getType());
        });
    }

    public void validateToManyRelationshipDoc(ToManyRelationshipsDoc toManyRelationshipDoc,
                                              Consumer<String> resourceIdValidator,
                                              Consumer<String> resourceTypeValidator,
                                              Consumer<Object> metaValidator) {
        validateToManyRelationshipDoc(toManyRelationshipDoc, resourceIdValidator, resourceTypeValidator);
        toManyRelationshipDoc.getData().forEach(ri -> metaValidator.accept(ri.getMeta()));
    }

}
