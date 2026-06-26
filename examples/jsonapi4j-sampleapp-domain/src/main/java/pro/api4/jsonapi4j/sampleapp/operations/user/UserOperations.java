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
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.SingleResourceDocValidationBuilder.ToManyRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.SingleResourceDocValidationBuilder.ToOneRelationshipObjectValidationBuilder;
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
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CountryRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef.RelationshipType;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;
import static pro.api4.jsonapi4j.operation.validation.Validate.assertThat;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

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

    public static List<RelativeRef> parseRelations(List<ResourceIdentifierObject> data) {
        List<RelativeRef> relations = new ArrayList<>();
        for (ResourceIdentifierObject ri : ListUtils.emptyIfNull(data)) {
            parseRelationshipType(ri).ifPresent(rt -> relations.add(new RelativeRef(ri.getId(), rt)));
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
                            DefaultErrorCodes.INVALID_ENUM_VALUE,
                            "Meta 'relationshipType' object only accepts string values: " + Arrays.stream(RelationshipType.values()).map(Enum::name).collect(Collectors.joining(", "))
                    );
                }
            } else {
                throw new JsonApiRequestValidationException(
                        DefaultErrorCodes.INVALID_ENUM_VALUE,
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
            throw new ResourceNotFoundException(request.getResourceId(), new ResourceType(USERS));
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
                List<CountryRef> citizenshipRefs = citizenships.getData()
                        .stream()
                        .map(ResourceIdentifierObject::getId)
                        .map(CountryRef::new)
                        .toList();
                if (!citizenshipRefs.isEmpty()) {
                    userDb.updateUserCitizenships(userId, citizenshipRefs);
                }
            }
            ToOneRelationshipObject placeOfBirth = (ToOneRelationshipObject) relationships.get(PLACE_OF_BIRTH);
            if (placeOfBirth != null) {
                userDb.updateUserPlaceOfBirth(userId, new CountryRef(placeOfBirth.getData().getId()));
            }
            ToManyRelationshipObject relatives = (ToManyRelationshipObject) relationships.get(RELATIVES);
            if (relatives != null) {
                List<RelativeRef> relations = parseRelations(relatives.getData());
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
        forRequest(request)
                .singleResourceBody(UserAttributes.class, body -> body
                        .withResourceTypeValidator(type -> type.isOneOf(USERS))
                        .withAttributesValidator(att -> {
                            att.isNotNull();
                            att.field("fullName", UserAttributes::getFullName).asString()
                                    .isNotBlank()
                                    .hasLengthLessThanOrEqualTo(128)
                                    .satisfies(name -> {
                                        assertThat(name.split("\\s+")[0]).isNotBlank().hasLengthBetween(1, 64);
                                        assertThat(name.split("\\s+")[1]).isNotBlank().hasLengthBetween(1, 64);
                                    });
                            att.field("email", UserAttributes::getEmail).asString()
                                    .isNotBlank()
                                    .isEmail();
                        })
                        .withToManyRelationship(CITIZENSHIPS, this::citizenshipsValidator)
                        .withToOneRelationship(PLACE_OF_BIRTH, this::placeOfBirthValidator)
                        .withToManyRelationship(RELATIVES, this::relativesValidator))
                .validate();
    }

    @Override
    public void validateUpdate(JsonApiRequest request) {
        forRequest(request)
                .singleResourceBody(UserAttributes.class, body -> body
                        .withResourceIdValidator(id -> id.exists(resourceId -> userDb.readById(resourceId) != null))
                        .withResourceTypeValidator(type -> type.isOneOf(USERS))
                        .withAttributesValidator(v -> {
                            v.ifPresent();
                            v.field("fullName", UserAttributes::getFullName).ifPresent().asString()
                                    .satisfies(name -> {
                                        assertThat(name.split("\\s+")[0]).isNotBlank().hasLengthBetween(1, 64);
                                        assertThat(name.split("\\s+")[1]).isNotBlank().hasLengthBetween(1, 64);
                                    });
                            v.field("email", UserAttributes::getEmail).ifPresent().asString().isEmail();
                        })
                        .withToManyRelationship(CITIZENSHIPS, this::citizenshipsValidator)
                        .withToOneRelationship(PLACE_OF_BIRTH, this::placeOfBirthValidator)
                        .withToManyRelationship(RELATIVES, this::relativesValidator))
                .validate();
    }

    @Override
    public void validateDelete(JsonApiRequest request) {
        assertThat(request.getResourceId())
                .exists(resourceId -> userDb.readById(resourceId) != null);
    }

    private void citizenshipsValidator(ToManyRelationshipObjectValidationBuilder v) {
        v.withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId))
                .withResourceTypeValidator(type -> type.isOneOf(COUNTRIES));
    }

    private void placeOfBirthValidator(ToOneRelationshipObjectValidationBuilder v) {
        v.withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId))
                .withResourceTypeValidator(type -> type.isOneOf(COUNTRIES));
    }

    private void relativesValidator(ToManyRelationshipObjectValidationBuilder v) {
        v.withResourceIdValidator(id -> id.exists(resourceId -> userDb.readById(resourceId) != null))
                .withResourceTypeValidator(type -> type.isOneOf(USERS))
                .withResourceIdentifierMetaValidator(meta -> meta.satisfies(UserOperations::validateRelationsMeta));
    }

}
