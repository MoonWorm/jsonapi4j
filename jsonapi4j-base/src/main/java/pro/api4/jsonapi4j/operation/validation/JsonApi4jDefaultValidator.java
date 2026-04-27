package pro.api4.jsonapi4j.operation.validation;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.exception.ConstraintViolationException;
import pro.api4.jsonapi4j.exception.InvalidLimitException;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;
import static pro.api4.jsonapi4j.operation.validation.ValidationProperties.DEFAULT_LIMIT_MAX_VALUE;
import static pro.api4.jsonapi4j.operation.validation.ValidationProperties.DEFAULT_MAX_ELEMENTS_IN_FILTER_PARAM;
import static pro.api4.jsonapi4j.operation.validation.ValidationProperties.DEFAULT_MAX_ELEMENTS_IN_INCLUDE_PARAM;
import static pro.api4.jsonapi4j.operation.validation.ValidationProperties.DEFAULT_MAX_ELEMENTS_IN_SORT_BY_PARAM;
import static pro.api4.jsonapi4j.operation.validation.ValidationProperties.DEFAULT_MAX_NUMBER_FILTER_PARAMS;
import static pro.api4.jsonapi4j.operation.validation.ValidationProperties.DEFAULT_RESOURCE_ID_MAX_LENGTH;

@NoArgsConstructor
@AllArgsConstructor
@Getter
public class JsonApi4jDefaultValidator {

    private int maxNumberFilterParams = Integer.parseInt(DEFAULT_MAX_NUMBER_FILTER_PARAMS);
    private int maxElementsInFilterParam = Integer.parseInt(DEFAULT_MAX_ELEMENTS_IN_FILTER_PARAM);
    private int resourceIdMaxLength = Integer.parseInt(DEFAULT_RESOURCE_ID_MAX_LENGTH);
    private long limitMaxValue = Long.parseLong(DEFAULT_LIMIT_MAX_VALUE);
    private int maxElementsInIncludeParam = Integer.parseInt(DEFAULT_MAX_ELEMENTS_IN_INCLUDE_PARAM);
    private int maxElementsInSortByParam = Integer.parseInt(DEFAULT_MAX_ELEMENTS_IN_SORT_BY_PARAM);

    public JsonApi4jDefaultValidator(ValidationProperties properties) {
        this.maxNumberFilterParams = properties.maxNumberFilterParams();
        this.maxElementsInFilterParam = properties.maxElementsInFilterParam();
        this.resourceIdMaxLength = properties.resourceIdMaxLength();
        this.limitMaxValue = properties.limitMaxValue();
        this.maxElementsInIncludeParam = properties.maxElementsInIncludeParam();
        this.maxElementsInSortByParam = properties.maxElementsInSortByParam();
    }

    public void validateNonNull(Object object, String parameter) {
        if (object == null) {
            throw new ConstraintViolationException("value can't be null", parameter);
        }
    }

    public void validateNonBlank(String value, String parameter) {
        if (StringUtils.isBlank(value)) {
            throw new ConstraintViolationException("value can't be blank", parameter);
        }
    }

    public void validateResourceId(String resourceId) {
        validateResourceId(resourceId, "resource id");
    }

    public void validateResourceId(String resourceId, String parameterName) {
        if (StringUtils.isBlank(resourceId)) {
            return;
        }
        if (resourceId.length() > resourceIdMaxLength) {
            throw new ConstraintViolationException(
                    MessageFormat.format("resource id length can''t be more than {0}", resourceIdMaxLength),
                    parameterName
            );
        }
    }

    public void validateResourceTypeAnyOf(String resourceTypeToEvaluate,
                                          Set<String> validResourceTypes) {
        for (String validResourceType : validResourceTypes) {
            if (validResourceType.equalsIgnoreCase(resourceTypeToEvaluate)) {
                return;
            }
        }
        throw new ConstraintViolationException(
                MessageFormat.format("resource type ''{0}'' not supported, available resource types: [{1}]", resourceTypeToEvaluate, String.join(", ", validResourceTypes)),
                "resourceType"
        );
    }

    public void validateFilterParams(Map<String, List<String>> filterParams) {
        if (filterParams.size() > maxNumberFilterParams) {
            throw new ConstraintViolationException(
                    MessageFormat.format("max number of filter params exceeded: {0}", maxNumberFilterParams),
                    "filter[*]"
            );
        }
        filterParams.forEach((filterName, filterValues) -> {
            if (filterValues.size() > maxElementsInFilterParam) {
                throw new ConstraintViolationException(
                        MessageFormat.format("max number of filter param elements exceeded: {0}", maxElementsInFilterParam),
                        FiltersAwareRequest.getFilterParam(filterName)
                );
            }
            if (ID_FILTER_NAME.equals(filterName)) {
                if (!filterValues.isEmpty()) {
                    filterValues.forEach(this::validateResourceId);
                }
            }
        });
    }

    public void validateIncludes(List<String> includes) {
        if (includes.size() > maxElementsInIncludeParam) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("Include value shouldn''t have more than {0} elements", maxElementsInIncludeParam),
                    IncludeAwareRequest.INCLUDE_PARAM
            );
        }
    }

    public void validateSortBy(Map<String, SortAwareRequest.SortOrder> sortBy) {
        if (sortBy.size() > maxElementsInSortByParam) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("Sort value shouldn''t have more than {0} elements", maxElementsInSortByParam),
                    SortAwareRequest.SORT_PARAM
            );
        }
    }

    public void validateLimit(String limit) {
        if (StringUtils.isBlank(limit)) {
            return;
        }
        try {
            long value = Long.parseLong(limit);
            if (value > limitMaxValue) {
                throw new InvalidLimitException(limit, MessageFormat.format("max allowed limit is {0}", limitMaxValue));
            }
        } catch (NumberFormatException nfe) {
            throw new InvalidLimitException(limit, "limit value must be a number");
        }
    }

    public void validateSingleResourceDoc(SingleResourceDoc<?> singleResourceDoc) {
        if (singleResourceDoc.getData() == null) {
            throw new ConstraintViolationException("'data' is null", "data");
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
            throw new ConstraintViolationException("'data' is null", "data");
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
