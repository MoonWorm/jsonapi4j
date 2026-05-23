package pro.api4.jsonapi4j.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.JsonApiBuildInRequestValidator;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.InvalidLimitException;
import pro.api4.jsonapi4j.exception.InvalidPayloadException;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;
import pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator;
import pro.api4.jsonapi4j.operation.validation.ValidationAssertions;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;
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
public class DefaultJsonApiBuildInRequestValidator implements JsonApiBuildInRequestValidator {

    private final DomainRegistry domainRegistry;
    private final ObjectMapper objectMapper;

    private final ValidationProperties properties;

    @Override
    public void validateReadResourceById(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue))
                .parameters(params -> params
                        .withIncludeValidator(this::validateIncludesCount)
                        .withFiltersValidator(this::validateFilterParams)
                        .withSortValidator(this::validateSortByCount))
                .validate();
    }

    @Override
    public void validateReadMultipleResources(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType))
                .parameters(params -> params
                        .withIncludeValidator(this::validateIncludesCount)
                        .withFiltersValidator(this::validateFilterParams)
                        .withLimitValidator(this::validateLimitValue)
                        .withSortValidator(this::validateSortByCount))
                .validate();
    }

    @Override
    public void validateCreateResource(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType))
                .singleResourceBody(body -> body
                        .withDataValidator(data -> ValidationAssertions.validateNonNull(data, ErrorSources.pointer().data().toPointer()))
                        .withResourceIdValidator(ValidationAssertions::validateIsNull)
                        .withResourceTypeValidator(type -> {
                            ValidationAssertions.validateNonBlank(type);
                            ValidationAssertions.validateEqualTo(type, request.getTargetResourceType().getType());
                        })
                        .withRelationshipsValidator(relationships ->
                                validateRelationshipsStructure(request.getTargetResourceType(), request.getSingleResourceDocPayload(), relationships)))
                .validate();
    }

    @Override
    public void validateUpdateResource(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue))
                .singleResourceBody(body -> body
                        .withDataValidator(data -> ValidationAssertions.validateNonNull(data, ErrorSources.pointer().data().toPointer()))
                        .withResourceIdValidator(id -> {
                            ValidationAssertions.validateNonBlank(id);
                            validateResourceId(id);
                            ValidationAssertions.validateEqualTo(id, request.getResourceId());
                        })
                        .withResourceTypeValidator(type -> {
                            ValidationAssertions.validateNonBlank(type);
                            ValidationAssertions.validateEqualTo(type, request.getTargetResourceType().getType());
                        })
                        .withRelationshipsValidator(relationships ->
                                validateRelationshipsStructure(request.getTargetResourceType(), request.getSingleResourceDocPayload(), relationships)))
                .validate();
    }

    @Override
    public void validateDeleteResource(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue))
                .validate();
    }

    @Override
    public void validateReadToOneRelationship(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue)
                        .withRelationshipNameValidator(rn -> validateKnownRelationshipName(request, rn)))
                .parameters(params -> params
                        .withIncludeValidator(this::validateIncludesCount)
                        .withFiltersValidator(this::validateFilterParams)
                        .withSortValidator(this::validateSortByCount))
                .validate();
    }

    @Override
    public void validateUpdateToOneRelationship(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue)
                        .withRelationshipNameValidator(rn -> validateKnownRelationshipName(request, rn)))
                .toOneRelationshipBody(body -> body
                        .withResourceIdValidator(this::validateResourceIdentifierId)
                        .withResourceTypeValidator(this::validateResourceIdentifierType))
                .validate();
    }

    @Override
    public void validateReadToManyRelationship(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue)
                        .withRelationshipNameValidator(rn -> validateKnownRelationshipName(request, rn)))
                .parameters(params -> params
                        .withIncludeValidator(this::validateIncludesCount)
                        .withFiltersValidator(this::validateFilterParams)
                        .withLimitValidator(this::validateLimitValue)
                        .withSortValidator(this::validateSortByCount))
                .validate();
    }

    @Override
    public void validateUpdateToManyRelationship(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue)
                        .withRelationshipNameValidator(rn -> validateKnownRelationshipName(request, rn)))
                .toManyRelationshipBody(body -> body
                        .withResourceIdValidator(this::validateResourceIdentifierId)
                        .withResourceTypeValidator(this::validateResourceIdentifierType))
                .validate();
    }

    @Override
    public void validateAddToManyRelationship(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue)
                        .withRelationshipNameValidator(rn -> validateKnownRelationshipName(request, rn)))
                .toManyRelationshipBody(body -> body
                        .withResourceIdValidator(this::validateResourceIdentifierId)
                        .withResourceTypeValidator(this::validateResourceIdentifierType))
                .validate();
    }

    @Override
    public void validateDeleteToManyRelationship(JsonApiRequest request) {
        JsonApiRequestValidator.forRequest(request)
                .path(path -> path
                        .withResourceTypeValidator(this::validateKnownResourceType)
                        .withResourceIdValidator(this::validateResourceIdValue)
                        .withRelationshipNameValidator(rn -> validateKnownRelationshipName(request, rn)))
                .toManyRelationshipBody(body -> body
                        .withResourceIdValidator(this::validateResourceIdentifierId)
                        .withResourceTypeValidator(this::validateResourceIdentifierType))
                .validate();
    }

    // --- Reusable validators for method references ---

    private void validateKnownResourceType(ResourceType resourceType) {
        Set<String> availableResourceTypes = domainRegistry.getResourceTypes()
                .stream()
                .map(ResourceType::getType)
                .collect(Collectors.toSet());
        ValidationAssertions.validateValueAnyOf(resourceType.getType(), availableResourceTypes);
    }

    private void validateResourceIdValue(String resourceId) {
        ValidationAssertions.validateNonBlank(resourceId);
        validateResourceId(resourceId);
    }

    private void validateIncludesCount(List<String> includes) {
        int max = properties.maxElementsInIncludeParam();
        if (ListUtils.emptyIfNull(includes).size() > max) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("Include value shouldn''t have more than {0} elements", max)
            );
        }
    }

    private void validateKnownRelationshipName(JsonApiRequest request, RelationshipName relationshipName) {
        Set<String> availableRelationships = domainRegistry.getRelationshipNames(request.getTargetResourceType())
                .stream()
                .map(RelationshipName::getName)
                .collect(Collectors.toSet());
        ValidationAssertions.validateValueAnyOf(relationshipName.getName(), availableRelationships);
    }

    private void validateResourceIdentifierId(String id) {
        ValidationAssertions.validateNonBlank(id);
        validateResourceId(id);
    }

    private void validateResourceIdentifierType(String type) {
        ValidationAssertions.validateNonBlank(type);
    }

    private void validateSortByCount(Map<String, SortAwareRequest.SortOrder> sortBy) {
        int max = properties.maxElementsInSortByParam();
        if (MapUtils.emptyIfNull(sortBy).size() > max) {
            throw new JsonApiRequestValidationException(
                    DefaultErrorCodes.ARRAY_LENGTH_TOO_LONG,
                    MessageFormat.format("Sort value shouldn''t have more than {0} elements", max)
            );
        }
    }

    private void validateLimitValue(Long limit) {
        if (limit == null) {
            return;
        }
        if (limit > properties.limitMaxValue()) {
            throw new InvalidLimitException(limit, MessageFormat.format("max allowed limit is {0}", properties.limitMaxValue()));
        }
    }

    private void validateRelationshipsStructure(ResourceType resourceType,
                                                SingleResourceDoc<?> doc,
                                                LinkedHashMap<String, RelationshipObject> relationships) {
        Set<String> availableToOneRelationshipNames = domainRegistry.getToOneRelationshipNames(resourceType)
                .stream()
                .map(RelationshipName::getName)
                .collect(Collectors.toSet());
        Set<String> availableToManyRelationshipNames = domainRegistry.getToManyRelationshipNames(resourceType)
                .stream()
                .map(RelationshipName::getName)
                .collect(Collectors.toSet());

        relationships.forEach((relationshipNameObj, relationshipDocObj) -> {
            if (relationshipNameObj instanceof String relationshipName) {
                if (availableToOneRelationshipNames.contains(relationshipName)) {
                    try {
                        ToOneRelationshipObject toOneRelationshipObject = objectMapper.convertValue(relationshipDocObj, ToOneRelationshipObject.class);
                        ValidationAssertions.validateNonNull(toOneRelationshipObject, ErrorSources.pointer().data().relationship(relationshipName).toPointer());
                        validateToOneRelationshipObjectStructure(toOneRelationshipObject, ErrorSources.pointer().data().relationship(relationshipName));
                    } catch (JsonApi4jException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InvalidPayloadException(
                                "Invalid To-One Relationship Object structure " + relationshipName,
                                ErrorSources.pointer().data().relationship(relationshipName).toPointer(),
                                doc
                        );
                    }
                } else if (availableToManyRelationshipNames.contains(relationshipName)) {
                    try {
                        ToManyRelationshipObject toManyRelationshipObject = objectMapper.convertValue(relationshipDocObj, ToManyRelationshipObject.class);
                        ValidationAssertions.validateNonNull(toManyRelationshipObject, ErrorSources.pointer().data().relationship(relationshipName).toPointer());
                        validateToManyRelationshipObjectStructure(toManyRelationshipObject, ErrorSources.pointer().data().relationship(relationshipName));
                    } catch (JsonApi4jException e) {
                        throw e;
                    } catch (Exception e) {
                        throw new InvalidPayloadException(
                                "Invalid To-Many Relationship Object structure " + relationshipName,
                                ErrorSources.pointer().data().relationship(relationshipName).toPointer(),
                                doc
                        );
                    }
                }
            } else {
                throw new InvalidPayloadException(
                        "Invalid relationship name " + relationshipNameObj,
                        ErrorSources.pointer().data().relationship(String.valueOf(relationshipNameObj)).toPointer(),
                        doc
                );
            }
        });
    }

    private void validateToOneRelationshipObjectStructure(ToOneRelationshipObject relationshipObject, ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder parameterPathPrefix) {
        ResourceIdentifierObject data = relationshipObject.getData();
        if (data != null) {
            validateResourceIdentifier(data, parameterPathPrefix);
        }
    }

    private void validateResourceIdentifier(ResourceIdentifierObject resourceIdentifier, ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder parameterPathPrefix) {
        ValidationAssertions.validateNonBlank(resourceIdentifier.getId(), parameterPathPrefix.id());
        validateResourceId(resourceIdentifier.getId(), parameterPathPrefix.id());
        ValidationAssertions.validateNonBlank(resourceIdentifier.getType(), parameterPathPrefix.type());
    }

    private void validateToManyRelationshipObjectStructure(ToManyRelationshipObject relationshipObject, ErrorSources.JsonPointerBuilder.DataJsonPointerBuilder parameterPathPrefix) {
        List<ResourceIdentifierObject> data = ListUtils.emptyIfNull(relationshipObject.getData());
        for (int i = 0; i < data.size(); i++) {
            ResourceIdentifierObject resourceIdentifier = data.get(i);
            validateResourceIdentifier(resourceIdentifier, parameterPathPrefix.index(i));
        }
    }

    private void validateFilterParams(Map<String, List<String>> filters) {
        Map<String, List<String>> filterParams = MapUtils.emptyIfNull(filters);
        int maxNumberFilterParams = properties.maxNumberFilterParams();
        if (filterParams.size() > maxNumberFilterParams) {
            throw new JsonApiRequestValidationException(
                    MessageFormat.format("max number of filter params exceeded: {0}", maxNumberFilterParams),
                    ErrorSources.parameter().filters()
            );
        }
        int maxElementsInFilterParam = properties.maxElementsInFilterParam();
        filterParams.forEach((filterName, filterValues) -> {
            if (filterValues.size() > maxElementsInFilterParam) {
                throw new JsonApiRequestValidationException(
                        MessageFormat.format("max number of filter param elements exceeded: {0}", maxElementsInFilterParam),
                        ErrorSources.parameter().filter(filterName)
                );
            }
            if (ID_FILTER_NAME.equals(filterName)) {
                if (!filterValues.isEmpty()) {
                    filterValues.forEach(resourceId -> validateResourceId(resourceId, ErrorSources.parameter().filter(ID_FILTER_NAME)));
                }
            }
        });
    }

    private void validateResourceId(String resourceId) {
        validateResourceId(resourceId, null);
    }

    private void validateResourceId(String resourceId, ErrorSources.Source source) {
        if (StringUtils.isBlank(resourceId)) {
            return;
        }
        if (resourceId.length() > properties.resourceIdMaxLength()) {
            throw new JsonApiRequestValidationException(
                    MessageFormat.format("resource id length can''t be more than {0}", properties.resourceIdMaxLength()),
                    source
            );
        }
    }

}
