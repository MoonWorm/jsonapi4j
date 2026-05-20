package pro.api4.jsonapi4j.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.JsonApiRequestValidator;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.InvalidLimitException;
import pro.api4.jsonapi4j.exception.InvalidPayloadException;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidatorHolder;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;

import java.text.MessageFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation.ID_FILTER_NAME;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class DefaultJsonApiRequestValidator implements JsonApiRequestValidator {

    private final DomainRegistry domainRegistry;
    private final ObjectMapper objectMapper;

    private final ValidationProperties properties;

    @Override
    public void validateReadResourceById(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
        validateIncludes(request);
        validateFilterParams(request);
        validateSortBy(request);
    }

    @Override
    public void validateReadMultipleResources(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateIncludes(request);
        validateFilterParams(request);
        validatePaginationParams(request);
        validateSortBy(request);
    }

    @Override
    public void validateCreateResource(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdIsNullInBody(request);
        validateSingleResourceDocPayloadStructure(request);
    }

    @Override
    public void validateUpdateResource(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
        validateResourceIdFromBodyMatchingOneInThePath(request);
        validateSingleResourceDocPayloadStructure(request);
    }

    @Override
    public void validateDeleteResource(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
    }

    @Override
    public void validateReadToOneRelationship(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipNameInThePath(request);
        validateIncludes(request);
        validateFilterParams(request);
        validateSortBy(request);
    }

    @Override
    public void validateUpdateToOneRelationship(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipNameInThePath(request);
        validateToOneRelationshipDocPayloadStructure(request);
    }

    @Override
    public void validateReadToManyRelationship(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipNameInThePath(request);
        validateIncludes(request);
        validateFilterParams(request);
        validatePaginationParams(request);
        validateSortBy(request);
    }

    @Override
    public void validateUpdateToManyRelationship(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipNameInThePath(request);
        validateToManyRelationshipDocPayloadStructure(request);
    }

    @Override
    public void validateAddToManyRelationship(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipNameInThePath(request);
        validateToManyRelationshipDocPayloadStructure(request);
    }

    @Override
    public void validateDeleteToManyRelationship(JsonApiRequest request) {
        validateKnownResourceTypeInThePath(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipNameInThePath(request);
        validateToManyRelationshipDocPayloadStructure(request);
    }

    private void validateKnownResourceTypeInThePath(JsonApiRequest request) {
        Set<String> availableResourceTypes = domainRegistry.getResourceTypes()
                .stream()
                .map(ResourceType::getType)
                .collect(Collectors.toSet());
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateValueAnyOf(
                request.getTargetResourceType().getType(),
                availableResourceTypes,
                ErrorSources.url().path().resourceType()
        );
    }

    private void validateResourceIdInThePath(JsonApiRequest request) {
        String resourceId = request.getResourceId();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(resourceId, ErrorSources.url().path().resourceId());
        validateResourceId(resourceId, ErrorSources.url().path().resourceId());
    }

    private void validateKnownRelationshipNameInThePath(JsonApiRequest request) {
        Set<String> availableRelationships = domainRegistry.getRelationshipNames(request.getTargetResourceType())
                .stream()
                .map(RelationshipName::getName)
                .collect(Collectors.toSet());
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateValueAnyOf(
                request.getTargetRelationshipName().getName(),
                availableRelationships,
                ErrorSources.url().path().relationshipName()
        );
    }

    private void validateResourceIdIsNullInBody(JsonApiRequest request) {
        var doc = request.getSingleResourceDocPayload();
        var data = doc.getData();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(data, ErrorSources.payload().data().toParameter());
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateIsNull(data.getId(), ErrorSources.payload().data().id());
    }

    private void validateResourceIdFromBodyMatchingOneInThePath(JsonApiRequest request) {
        var doc = request.getSingleResourceDocPayload();
        var data = doc.getData();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(data, ErrorSources.payload().data().toParameter());

        String resourceIdFromThePath = request.getResourceId();
        String resourceIdFromThePayload = data.getId();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(resourceIdFromThePayload, ErrorSources.payload().data().id());
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateEqualTo(resourceIdFromThePayload, resourceIdFromThePath,ErrorSources.payload().data().id());
    }

    // TODO: Introduce Parameter builder API and use it across the framework, fix all the tests
    // TODO: REUSE COMMON VALIDATOR PAYLOAD METHODS?
    private void validateSingleResourceDocPayloadStructure(JsonApiRequest request) {
        ResourceType resourceType = request.getTargetResourceType();
        var doc = request.getSingleResourceDocPayload();
        var data = doc.getData();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(data, ErrorSources.payload().data().toParameter());

        validateResourceId(data.getId(), ErrorSources.payload().data().id());

        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(data.getType(), ErrorSources.payload().data().type());
        // type in the payload should match one in the path
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateEqualTo(data.getType(), resourceType.getType(), ErrorSources.payload().data().type());

        if (MapUtils.isNotEmpty(data.getRelationships())) {
            Set<String> availableToOneRelationshipNames = domainRegistry.getToOneRelationshipNames(resourceType)
                    .stream()
                    .map(RelationshipName::getName)
                    .collect(Collectors.toSet());
            Set<String> availableToManyRelationshipNames = domainRegistry.getToManyRelationshipNames(resourceType)
                    .stream()
                    .map(RelationshipName::getName)
                    .collect(Collectors.toSet());

            data.getRelationships().forEach((relationshipNameObj, relationshipDocObj) -> {

                if (relationshipNameObj instanceof String relationshipName) {
                    if (availableToOneRelationshipNames.contains(relationshipName)) {
                        try {
                            ToOneRelationshipObject toOneRelationshipObject = objectMapper.convertValue(relationshipDocObj, ToOneRelationshipObject.class);
                            JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(toOneRelationshipObject, ErrorSources.payload().data().relationship(relationshipName).toParameter());
                            validateToOneRelationshipObjectStructure(toOneRelationshipObject, ErrorSources.payload().data().relationship(relationshipName));
                        } catch (JsonApi4jException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new InvalidPayloadException(
                                    "Invalid To-One Relationship Object structure " + relationshipName,
                                    ErrorSources.payload().data().relationship(relationshipName).toParameter(),
                                    doc
                            );
                        }
                    } else if (availableToManyRelationshipNames.contains(relationshipName)) {
                        try {
                            ToManyRelationshipObject toManyRelationshipObject = objectMapper.convertValue(relationshipDocObj, ToManyRelationshipObject.class);
                            JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(toManyRelationshipObject, ErrorSources.payload().data().relationship(relationshipName).toParameter());
                            validateToManyRelationshipObjectStructure(toManyRelationshipObject, ErrorSources.payload().data().relationship(relationshipName));
                        } catch (JsonApi4jException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new InvalidPayloadException(
                                    "Invalid To-Many Relationship Object structure " + relationshipName,
                                    ErrorSources.payload().data().relationship(relationshipName).toParameter(),
                                    doc
                            );
                        }
                    }
                } else {
                    throw new InvalidPayloadException(
                            "Invalid relationship name " + relationshipNameObj,
                            ErrorSources.payload().data().relationship(String.valueOf(relationshipNameObj)).toParameter(),
                            doc
                    );
                }
            });
        }
    }

    private void validateToOneRelationshipObjectStructure(ToOneRelationshipObject relationshipObject, ErrorSources.PayloadSources.PayloadDataSources parameterPathPrefix) {
        ResourceIdentifierObject data = relationshipObject.getData();
        if (data != null) {
            validateResourceIdentifier(data, parameterPathPrefix);
        }
    }

    private void validateResourceIdentifier(ResourceIdentifierObject resourceIdentifier, ErrorSources.PayloadSources.PayloadDataSources parameterPathPrefix) {
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(resourceIdentifier.getId(), parameterPathPrefix.id());
        validateResourceId(resourceIdentifier.getId(), parameterPathPrefix.id());
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(resourceIdentifier.getType(), parameterPathPrefix.type());
    }

    private void validateToManyRelationshipObjectStructure(ToManyRelationshipObject relationshipObject, ErrorSources.PayloadSources.PayloadDataSources parameterPathPrefix) {
        List<ResourceIdentifierObject> data = ListUtils.emptyIfNull(relationshipObject.getData());
        for (int i = 0; i < data.size(); i++) {
            ResourceIdentifierObject resourceIdentifier = data.get(i);
            validateResourceIdentifier(resourceIdentifier, parameterPathPrefix.index(i));
        }
    }

    private void validateToOneRelationshipDocPayloadStructure(JsonApiRequest request) {
        ToOneRelationshipDoc doc = request.getToOneRelationshipDocPayload();
        validateToOneRelationshipObjectStructure(doc, ErrorSources.payload().data());
    }

    private void validateToManyRelationshipDocPayloadStructure(JsonApiRequest request) {
        ToManyRelationshipsDoc doc = request.getToManyRelationshipDocPayload();
        validateToManyRelationshipObjectStructure(doc, ErrorSources.payload().data());
    }

    private void validateIncludes(JsonApiRequest request) {
        int maxElementsInIncludeParam = properties.maxElementsInIncludeParam();
        if (ListUtils.emptyIfNull(request.getOriginalIncludes()).size() > maxElementsInIncludeParam) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("Include value shouldn''t have more than {0} elements", maxElementsInIncludeParam),
                    ErrorSources.url().queryParams().include()
            );
        }
    }

    private void validateSortBy(JsonApiRequest request) {
        Map<String, SortAwareRequest.SortOrder> sortBy = request.getSortBy();
        int maxElementsInSortByParam = properties.maxElementsInSortByParam();
        if (MapUtils.emptyIfNull(sortBy).size() > maxElementsInSortByParam) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("Sort value shouldn''t have more than {0} elements", maxElementsInSortByParam),
                    ErrorSources.url().queryParams().sort()
            );
        }
    }

    private void validateFilterParams(JsonApiRequest request) {
        Map<String, List<String>> filterParams = MapUtils.emptyIfNull(request.getFilters());
        int maxNumberFilterParams = properties.maxNumberFilterParams();
        if (filterParams.size() > maxNumberFilterParams) {
            throw new JsonApiRequestValidationException(
                    MessageFormat.format("max number of filter params exceeded: {0}", maxNumberFilterParams),
                    ErrorSources.url().queryParams().filters()
            );
        }
        int maxElementsInFilterParam = properties.maxElementsInFilterParam();
        filterParams.forEach((filterName, filterValues) -> {
            if (filterValues.size() > maxElementsInFilterParam) {
                throw new JsonApiRequestValidationException(
                        MessageFormat.format("max number of filter param elements exceeded: {0}", maxElementsInFilterParam),
                        ErrorSources.url().queryParams().filter(filterName)
                );
            }
            if (ID_FILTER_NAME.equals(filterName)) {
                if (!filterValues.isEmpty()) {
                    filterValues.forEach(resourceId -> validateResourceId(resourceId, ErrorSources.url().queryParams().filter(ID_FILTER_NAME)));
                }
            }
        });
    }

    private void validatePaginationParams(JsonApiRequest request) {
        validateLimit(request);
    }

    private void validateLimit(JsonApiRequest request) {
        Long limit = request.getLimit();
        if (limit == null) {
            return;
        }
        if (limit > properties.limitMaxValue()) {
            throw new InvalidLimitException(limit, MessageFormat.format("max allowed limit is {0}", properties.limitMaxValue()));
        }
    }

    private void validateResourceId(String resourceId, ErrorSources.ParameterPath parameterPath) {
        if (StringUtils.isBlank(resourceId)) {
            return;
        }
        if (resourceId.length() > properties.resourceIdMaxLength()) {
            throw new JsonApiRequestValidationException(
                    MessageFormat.format("resource id length can''t be more than {0}", properties.resourceIdMaxLength()),
                    parameterPath
            );
        }
    }

}
