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
import pro.api4.jsonapi4j.exception.ConstraintViolationException;
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
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidatorHolder;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest;

import java.text.MessageFormat;
import java.util.LinkedHashMap;
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
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
        validateIncludes(request);
        validateFilterParams(request);
        validateSortBy(request);
    }

    @Override
    public void validateReadMultipleResources(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateIncludes(request);
        validateFilterParams(request);
        validatePaginationParams(request);
        validateSortBy(request);
    }

    @Override
    public void validateCreateResource(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateSingleResourceDocPayloadStructure(request);
    }

    @Override
    public void validateUpdateResource(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
        validateResourceIdFromBodyMatchingOneInThePath(request);
        validateSingleResourceDocPayloadStructure(request);
    }

    @Override
    public void validateDeleteResource(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
    }

    @Override
    public void validateReadToOneRelationship(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipName(request);
        validateIncludes(request);
        validateFilterParams(request);
        validateSortBy(request);
    }

    @Override
    public void validateUpdateToOneRelationship(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipName(request);
        validateToOneRelationshipDocPayloadStructure(request);
    }

    @Override
    public void validateReadToManyRelationship(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipName(request);
        validateIncludes(request);
        validateFilterParams(request);
        validatePaginationParams(request);
        validateSortBy(request);
    }

    @Override
    public void validateUpdateToManyRelationship(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipName(request);
        validateToManyRelationshipDocPayloadStructure(request);
    }

    @Override
    public void validateAddToManyRelationship(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipName(request);
        validateToManyRelationshipDocPayloadStructure(request);
    }

    @Override
    public void validateDeleteToManyRelationship(JsonApiRequest request) {
        validateKnownResourceType(request);
        validateResourceIdInThePath(request);
        validateKnownRelationshipName(request);
        validateToManyRelationshipDocPayloadStructure(request);
    }

    private void validateKnownResourceType(JsonApiRequest request) {
        Set<String> availableResourceTypes = domainRegistry.getResourceTypes()
                .stream()
                .map(ResourceType::getType)
                .collect(Collectors.toSet());
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateValueAnyOf(
                request.getTargetResourceType().getType(),
                availableResourceTypes,
                "path -> {resourceType}"
        );
    }

    private void validateResourceIdInThePath(JsonApiRequest request) {
        String resourceId = request.getResourceId();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(resourceId, "path -> {id}");
        validateResourceId(resourceId, "path -> {resourceId}");
    }

    private void validateKnownRelationshipName(JsonApiRequest request) {
        Set<String> availableRelationships = domainRegistry.getRelationshipNames(request.getTargetResourceType())
                .stream()
                .map(RelationshipName::getName)
                .collect(Collectors.toSet());
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateValueAnyOf(
                request.getTargetRelationshipName().getName(),
                availableRelationships,
                "path -> {relationshipName}"
        );
    }

    private void validateResourceIdFromBodyMatchingOneInThePath(JsonApiRequest request) {
        SingleResourceDoc<ResourceObject<LinkedHashMap, LinkedHashMap>> doc = request.getSingleResourceDocPayload();
        ResourceObject<LinkedHashMap, LinkedHashMap> data = doc.getData();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(data, "body -> data");

        String resourceIdFromThePath = request.getResourceId();
        String resourceIdFromThePayload = data.getId();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(resourceIdFromThePayload, "body -> data -> id");
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateEqualTo(resourceIdFromThePayload, resourceIdFromThePath,"body -> data -> id");
    }

    // TODO: Introduce Parameter builder API and use it across the framework, fix all the tests
    // TODO: REUSE COMMON VALIDATOR PAYLOAD METHODS?
    private void validateSingleResourceDocPayloadStructure(JsonApiRequest request) {
        ResourceType resourceType = request.getTargetResourceType();
        SingleResourceDoc<ResourceObject<LinkedHashMap, LinkedHashMap>> doc = request.getSingleResourceDocPayload();
        ResourceObject<LinkedHashMap, LinkedHashMap> data = doc.getData();
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(data, "body -> data");

        validateResourceId(data.getId(), "body -> data -> id");

        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(data.getType(), "body -> data -> type");
        // type in the payload should match one in the path
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateEqualTo(data.getType(), resourceType.getType(), "body -> data -> type");

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
                            JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(toOneRelationshipObject, "body -> data -> relationships -> " + relationshipName + " -> data");
                            validateToOneRelationshipObjectStructure(toOneRelationshipObject, "body -> data -> relationships -> " + relationshipName + " -> data");
                        } catch (JsonApi4jException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new InvalidPayloadException(
                                    "Invalid To-One Relationship Object structure " + relationshipName,
                                    "body -> data -> relationships -> " + relationshipName,
                                    doc
                            );
                        }
                    } else if (availableToManyRelationshipNames.contains(relationshipName)) {
                        try {
                            ToManyRelationshipObject toManyRelationshipObject = objectMapper.convertValue(relationshipDocObj, ToManyRelationshipObject.class);
                            JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonNull(toManyRelationshipObject, "body -> data -> relationships -> " + relationshipName);
                            validateToManyRelationshipObjectStructure(toManyRelationshipObject, "body -> data -> relationships -> " + relationshipName);
                        } catch (JsonApi4jException e) {
                            throw e;
                        } catch (Exception e) {
                            throw new InvalidPayloadException(
                                    "Invalid To-Many Relationship Object structure " + relationshipName,
                                    "body -> data -> relationships -> " + relationshipName,
                                    doc
                            );
                        }
                    }
                } else {
                    throw new InvalidPayloadException(
                            "Invalid relationship name " + relationshipNameObj,
                            "body -> data -> relationships -> " + relationshipNameObj,
                            doc
                    );
                }
            });
        }
    }

    private void validateToOneRelationshipObjectStructure(ToOneRelationshipObject relationshipObject, String parameterPathPrefix) {
        ResourceIdentifierObject data = relationshipObject.getData();
        if (data != null) {
            validateResourceIdentifier(data, parameterPathPrefix);
        }
    }

    private void validateResourceIdentifier(ResourceIdentifierObject resourceIdentifier, String parameterPathPrefix) {
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(resourceIdentifier.getId(), parameterPathPrefix + " -> id");
        validateResourceId(resourceIdentifier.getId(), parameterPathPrefix + " -> id");
        JsonApi4jDefaultValidatorHolder.INSTANCE.validateNonBlank(resourceIdentifier.getType(), parameterPathPrefix + " -> type");
    }

    private void validateToManyRelationshipObjectStructure(ToManyRelationshipObject relationshipObject, String parameterPathPrefix) {
        List<ResourceIdentifierObject> data = ListUtils.emptyIfNull(relationshipObject.getData());
        for (int i = 0; i < data.size(); i++) {
            ResourceIdentifierObject resourceIdentifier = data.get(i);
            validateResourceIdentifier(resourceIdentifier, parameterPathPrefix + " -> data[" + i + "]");
        }
    }

    private void validateToOneRelationshipDocPayloadStructure(JsonApiRequest request) {
        ToOneRelationshipDoc doc = request.getToOneRelationshipDocPayload();
        validateToOneRelationshipObjectStructure(doc, "body -> data");
    }

    private void validateToManyRelationshipDocPayloadStructure(JsonApiRequest request) {
        ToManyRelationshipsDoc doc = request.getToManyRelationshipDocPayload();
        validateToManyRelationshipObjectStructure(doc, "body");
    }

    private void validateIncludes(JsonApiRequest request) {
        int maxElementsInIncludeParam = properties.maxElementsInIncludeParam();
        if (ListUtils.emptyIfNull(request.getOriginalIncludes()).size() > maxElementsInIncludeParam) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("Include value shouldn''t have more than {0} elements", maxElementsInIncludeParam),
                    IncludeAwareRequest.INCLUDE_PARAM
            );
        }
    }

    private void validateSortBy(JsonApiRequest request) {
        Map<String, SortAwareRequest.SortOrder> sortBy = request.getSortBy();
        int maxElementsInSortByParam = properties.maxElementsInSortByParam();
        if (MapUtils.emptyIfNull(sortBy).size() > maxElementsInSortByParam) {
            throw new ConstraintViolationException(
                    DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("Sort value shouldn''t have more than {0} elements", maxElementsInSortByParam),
                    SortAwareRequest.SORT_PARAM
            );
        }
    }

    private void validateFilterParams(JsonApiRequest request) {
        Map<String, List<String>> filterParams = MapUtils.emptyIfNull(request.getFilters());
        int maxNumberFilterParams = properties.maxNumberFilterParams();
        if (filterParams.size() > maxNumberFilterParams) {
            throw new ConstraintViolationException(
                    MessageFormat.format("max number of filter params exceeded: {0}", maxNumberFilterParams),
                    "filter[*]"
            );
        }
        int maxElementsInFilterParam = properties.maxElementsInFilterParam();
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

    private void validateResourceId(String resourceId) {
        validateResourceId(resourceId, "resource id");
    }

    private void validateResourceId(String resourceId, String parameterName) {
        if (StringUtils.isBlank(resourceId)) {
            return;
        }
        if (resourceId.length() > properties.resourceIdMaxLength()) {
            throw new ConstraintViolationException(
                    MessageFormat.format("resource id length can''t be more than {0}", properties.resourceIdMaxLength()),
                    parameterName
            );
        }
    }

}
