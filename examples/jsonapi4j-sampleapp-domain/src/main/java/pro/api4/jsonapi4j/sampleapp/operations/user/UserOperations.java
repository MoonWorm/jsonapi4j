package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.JsonApiRequestValidationException;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator.SingleResourceDocValidationBuilder.ToManyRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator.SingleResourceDocValidationBuilder.ToOneRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlAccessTier;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.plugin.oas.operation.model.In;
import pro.api4.jsonapi4j.principal.tier.TierAdmin;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo.RelationshipType;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.CountryInputParamsValidator;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator.validateNonNull;
import static pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator.validateValueAnyOf;
import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship.CITIZENSHIPS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserPlaceOfBirthRelationship.PLACE_OF_BIRTH;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship.RELATIONSHIP_TYPE_META_KEY;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship.RELATIVES;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserResource.USERS;

@RequiredArgsConstructor
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    private final UserDb userDb;
    private final UserInputParamsValidator userValidator;

    public static Map<String, RelationshipType> parseRelations(List<ResourceIdentifierObject> data) {
        Map<String, RelationshipType> relations = new LinkedHashMap<>();
        for (ResourceIdentifierObject ri : ListUtils.emptyIfNull(data)) {
            parseRelationshipType(ri).ifPresent(rt -> relations.put(ri.getId(), rt));
        }
        return relations;
    }

    public static Optional<RelationshipType> parseRelationshipType(ResourceIdentifierObject ri) {
        Object meta = ri.getMeta();
        if (meta instanceof Map<?, ?> m) {
            Object relationshipTypeObj = m.get(RELATIONSHIP_TYPE_META_KEY);
            if (relationshipTypeObj instanceof String relationshipType && StringUtils.isNotBlank(relationshipType)) {
                return Optional.of(RelationshipType.valueOf(relationshipType.toUpperCase()));
            }
        }
        return Optional.empty();
    }

    public static void validateRelationsMeta(Object meta) {
        if (meta instanceof Map<?, ?> m) {
            Object relationshipTypeObj = m.get(RELATIONSHIP_TYPE_META_KEY);
            if (relationshipTypeObj == null) {
                return;
            }
            if (relationshipTypeObj instanceof String relationshipType && StringUtils.isNotBlank(relationshipType)) {
                try {
                    RelationshipType.valueOf(relationshipType.toUpperCase());
                } catch (Exception ex) {
                    throw new JsonApiRequestValidationException(
                            "Meta 'relationshipType' object only accepts string values: " + Arrays.stream(RelationshipType.values()).map(Enum::name).collect(Collectors.joining(", "))
                    );
                }
            } else {
                throw new JsonApiRequestValidationException(
                        "Meta 'relationshipType' object only accepts string values: " + Arrays.stream(RelationshipType.values()).map(Enum::name).collect(Collectors.joining(", "))
                );
            }
        }
    }

    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            ),
            parameters = {
                    @Parameter(
                            name = "id",
                            in = In.PATH,
                            description = "User unique identifier",
                            example = "3"
                    )
            }
    )
    @Override
    public UserDbEntity readById(JsonApiRequest request) {
        UserDbEntity userDbEntity = userDb.readById(request.getResourceId());
        if (userDbEntity == null) {
            throwResourceNotFoundException(request);
        }
        return userDbEntity;
    }

    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            ),
            parameters = {
                    @Parameter(
                            name = "filter[id]",
                            description = "Allows to filter users based on id attribute value",
                            example = "3",
                            array = true,
                            required = false
                    )
            }
    )
    @Override
    public PaginationAwareResponse<UserDbEntity> readPage(JsonApiRequest request) {
        if (request.getFilters().containsKey(ID_FILTER_NAME)) {
            return PaginationAwareResponse.fromItemsNotPageable(
                    userDb.readByIds(request.getFilters().get(ID_FILTER_NAME))
            );
        } else {
            if (StringUtils.isNotBlank(request.getCursor())) {
                UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(request.getCursor());
                return PaginationAwareResponse.cursorAware(
                        pagedResult.getEntities(),
                        pagedResult.getCursor()
                );
            } else if (request.getLimit() != null && request.getOffset() != null) {
                UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(request.getLimit(), request.getOffset());
                return PaginationAwareResponse.limitOffsetAware(
                        pagedResult.getEntities(),
                        pagedResult.getTotalItems()
                );
            } else {
                // fallback to 'null' cursor pagination
                UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(null);
                return PaginationAwareResponse.cursorAware(
                        pagedResult.getEntities(),
                        pagedResult.getCursor()
                );
            }

        }
    }

    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            )
    )
    @Override
    public UserDbEntity create(JsonApiRequest request) {
        var singleResourceDoc = request.getSingleResourceDocPayload(UserAttributes.class);
        UserAttributes att = singleResourceDoc.getData().getAttributes();
        UserDbEntity result = userDb.createUser(
                att.getFullName().split("\\s+")[0],
                att.getFullName().split("\\s+")[1],
                att.getEmail(),
                att.getCreditCardNumber()
        );
        updateUserRelationships(result.getId(), singleResourceDoc.getData().getRelationships());
        return result;
    }

    @AccessControl(
            authenticated = Authenticated.AUTHENTICATED,
            ownership = @AccessControlOwnership(
                    ownerIdExtractor = ResourceIdFromUrlPathExtractor.class
            )
    )
    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            )
    )
    @Override
    public void update(JsonApiRequest request) {
        var payload = request.getSingleResourceDocPayload(UserAttributes.class);
        UserAttributes att = payload.getData().getAttributes();
        String userId = payload.getData().getId();
        if (att != null) {
            String firstName = null;
            String lastName = null;
            if (StringUtils.isNotBlank(att.getFullName())) {
                firstName = att.getFullName().split("\\s+")[0];
                lastName = att.getFullName().split("\\s+")[1];
            }
            userDb.updateUser(
                    userId,
                    firstName,
                    lastName,
                    att.getEmail(),
                    att.getCreditCardNumber()
            );
        }
        updateUserRelationships(userId, payload.getData().getRelationships());
    }

    private void updateUserRelationships(String userId,
                                         LinkedHashMap<String, RelationshipObject> relationships) {
        if (relationships != null) {
            ToManyRelationshipObject citizenships = (ToManyRelationshipObject) relationships.get(CITIZENSHIPS);
            if (citizenships != null) {
                List<String> countryIds = citizenships.getData()
                        .stream()
                        .map(ResourceIdentifierObject::getId)
                        .toList();
                if (!countryIds.isEmpty()) {
                    userDb.updateUserCitizenships(userId, countryIds);
                }
            }
            ToOneRelationshipObject placeOfBirth = (ToOneRelationshipObject) relationships.get(PLACE_OF_BIRTH);
            if (placeOfBirth != null) {
                userDb.updateUserPlaceOfBirth(userId, placeOfBirth.getData().getId());
            }
            ToManyRelationshipObject relatives = (ToManyRelationshipObject) relationships.get(RELATIVES);
            if (relatives != null) {
                Map<String, RelationshipType> relations = parseRelations(relatives.getData());
                if (!relations.isEmpty()) {
                    userDb.updateUserRelatives(userId, relations);
                }
            }
        }
    }

    @AccessControl(tier = @AccessControlAccessTier(TierAdmin.ADMIN_ACCESS_TIER))
    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            )
    )
    @Override
    public void delete(JsonApiRequest request) {
        userDb.deleteUser(request.getResourceId());
    }

    @Override
    public void validateCreate(JsonApiRequest request) {
        var singleResourceDoc = request.getSingleResourceDocPayload(UserAttributes.class);
        getValidator().validateSingleResourceDoc(singleResourceDoc)
                .withResourceTypeValidator(resourceType -> validateValueAnyOf(resourceType, Set.of(USERS)))
                .withAttributesValidator(att -> {
                    validateNonNull(att, ErrorSources.pointer().data().attributes());
                    validateNonNull(att.getFullName(), ErrorSources.pointer().data().attributes("fullName"));
                    userValidator.validateFirstName(att.getFullName().split("\\s+")[0]);
                    userValidator.validateLastName(att.getFullName().split("\\s+")[1]);
                    userValidator.validateEmail(att.getEmail());
                })
                .withToManyRelationshipValidator(CITIZENSHIPS, citizenshipsValidator())
                .withToOneRelationshipValidator(PLACE_OF_BIRTH, placeOfBirthValidator())
                .withToManyRelationshipValidator(RELATIVES, relativesValidator())
                .validate();
    }

    @Override
    public void validateUpdate(JsonApiRequest request) {
        var singleResourceDoc = request.getSingleResourceDocPayload(UserAttributes.class);
        getValidator().validateSingleResourceDoc(singleResourceDoc)
                .withResourceIdValidator(resourceId -> {
                    if (userDb.readById(resourceId) == null) {
                        throwResourceNotFoundException(request);
                    }
                })
                .withResourceTypeValidator(resourceType -> validateValueAnyOf(resourceType, Set.of(USERS)))
                .withAttributesValidator(att -> {
                    if (att != null) {
                        if (att.getFullName() != null) {
                            userValidator.validateFirstName(att.getFullName().split("\\s+")[0]);
                            userValidator.validateLastName(att.getFullName().split("\\s+")[1]);
                        }
                        if (att.getEmail() != null) {
                            userValidator.validateEmail(att.getEmail());
                        }
                    }
                })
                .withToManyRelationshipValidator(CITIZENSHIPS, citizenshipsValidator())
                .withToOneRelationshipValidator(PLACE_OF_BIRTH, placeOfBirthValidator())
                .withToManyRelationshipValidator(RELATIVES, relativesValidator())
                .validate();
    }

    @Override
    public void validateDelete(JsonApiRequest request) {
        if (userDb.readById(request.getResourceId()) == null) {
            throwResourceNotFoundException(request);
        }
    }


    private ToManyRelationshipObjectValidationBuilder citizenshipsValidator() {
        return getValidator().validateToManyRelationshipsObject()
                .withResourceIdValidator(CountryInputParamsValidator::validateCountryId)
                .withResourceTypeValidator(resourceType -> validateValueAnyOf(resourceType, Set.of(COUNTRIES)));
    }

    private ToOneRelationshipObjectValidationBuilder placeOfBirthValidator() {
        return getValidator().validateToOneRelationshipObject()
                .withResourceIdValidator(CountryInputParamsValidator::validateCountryId)
                .withResourceTypeValidator(resourceType -> validateValueAnyOf(resourceType, Set.of(COUNTRIES)));
    }

    private ToManyRelationshipObjectValidationBuilder relativesValidator() {
        return getValidator().validateToManyRelationshipsObject()
                .withResourceIdValidator(resourceId -> {
                    if (userDb.readById(resourceId) == null) {
                        throw new ResourceNotFoundException(resourceId, new ResourceType(USERS));
                    }
                })
                .withResourceTypeValidator(resourceType -> validateValueAnyOf(resourceType, Set.of(USERS)))
                .withResourceIdentifierMetaValidator(UserOperations::validateRelationsMeta);

    }

}
