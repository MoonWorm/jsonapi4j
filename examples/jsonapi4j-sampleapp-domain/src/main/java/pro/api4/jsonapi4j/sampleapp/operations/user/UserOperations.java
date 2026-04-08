package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jConstraintViolationException;
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
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationships;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship.RELATIONSHIP_TYPE_META_KEY;

@RequiredArgsConstructor
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    private final UserDb userDb;
    private final UserInputParamsValidator userValidator;
    private final CountryInputParamsValidator countryValidator;

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
        var payload = request.getSingleResourceDocPayload(UserAttributes.class, UserRelationships.class);
        UserAttributes att = payload.getData().getAttributes();
        UserDbEntity result = userDb.createUser(
                att.getFullName().split("\\s+")[0],
                att.getFullName().split("\\s+")[1],
                att.getEmail(),
                att.getCreditCardNumber()
        );
        updateUserRelationships(result.getId(), payload.getData().getRelationships());
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
        var payload = request.getSingleResourceDocPayload(UserAttributes.class, UserRelationships.class);
        UserAttributes att = payload.getData().getAttributes();
        String userId = payload.getData().getId();
        if (att != null) {
            String firstName = null;
            String lastName = null;
            if (StringUtils.isBlank(att.getFullName())) {
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
                                         UserRelationships relationships) {
        if (relationships != null) {
            if (relationships.getCitizenships() != null) {
                List<String> countryIds = relationships.getCitizenships().getData()
                        .stream()
                        .map(ResourceIdentifierObject::getId)
                        .toList();
                if (!countryIds.isEmpty()) {
                    userDb.updateUserCitizenships(userId, countryIds);
                }
            }
            if (relationships.getPlaceOfBirth() != null) {
                userDb.updateUserPlaceOfBirth(userId, relationships.getPlaceOfBirth().getData().getId());
            }
            if (relationships.getRelatives() != null) {
                Map<String, RelationshipType> relations = new HashMap<>();
                for (ResourceIdentifierObject ri : relationships.getRelatives().getData()) {
                    Object meta = ri.getMeta();
                    if (meta instanceof Map<?, ?> m) {
                        Object relationshipTypeObj = m.get(RELATIONSHIP_TYPE_META_KEY);
                        if (relationshipTypeObj instanceof String relationshipType && StringUtils.isNotBlank(relationshipType)) {
                            try {
                                relations.put(ri.getId(), RelationshipType.valueOf(relationshipType.toUpperCase()));
                            } catch (Exception ex) {
                                // do nothing
                            }
                        }
                    }
                }
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
        ResourceOperations.super.validateCreate(request);
        var payload = request.getSingleResourceDocPayload(UserAttributes.class, UserRelationships.class);
        UserAttributes att = payload.getData().getAttributes();
        if (att == null) {
            throw new JsonApi4jConstraintViolationException("'attributes' is null", "attributes");
        }
        if (att.getFullName() == null) {
            throw new JsonApi4jConstraintViolationException("'attributes.fullName' is null", "attributes -> fullName");
        }
        userValidator.validateFirstName(att.getFullName().split("\\s+")[0]);
        userValidator.validateLastName(att.getFullName().split("\\s+")[1]);
        userValidator.validateEmail(att.getEmail());
        validateRelationships(payload.getData().getRelationships());
    }

    @Override
    public void validateUpdate(JsonApiRequest request) {
        ResourceOperations.super.validateUpdate(request);
        var payload = request.getSingleResourceDocPayload(UserAttributes.class, UserRelationships.class);
        UserAttributes att = payload.getData().getAttributes();
        if (att != null) {
            if (att.getFullName() != null) {
                userValidator.validateFirstName(att.getFullName().split("\\s+")[0]);
                userValidator.validateLastName(att.getFullName().split("\\s+")[1]);
            }
            userValidator.validateEmail(att.getEmail());
        }
        validateRelationships(payload.getData().getRelationships());
    }

    private void validateRelationships(UserRelationships rel) {
        if (rel != null) {
            if (rel.getCitizenships() != null) {
                ListUtils.emptyIfNull(rel.getCitizenships().getData())
                        .stream()
                        .map(ResourceIdentifierObject::getId)
                        .forEach(countryValidator::validateCountryId);
                ListUtils.emptyIfNull(rel.getCitizenships().getData())
                        .stream()
                        .map(ResourceIdentifierObject::getType)
                        .forEach(t -> {
                            if (!"countries".equalsIgnoreCase(t)) {
                                throw new JsonApi4jConstraintViolationException("Resource type must be 'countries'", "type");
                            }
                        });
            }
            if (rel.getPlaceOfBirth() != null && rel.getPlaceOfBirth().getData() != null) {
                if (StringUtils.isBlank(rel.getPlaceOfBirth().getData().getId())) {
                    countryValidator.validateCountryId(rel.getPlaceOfBirth().getData().getId());
                }
                if (!"countries".equalsIgnoreCase(rel.getPlaceOfBirth().getData().getType())) {
                    throw new JsonApi4jConstraintViolationException("Resource type must be 'countries'", "type");
                }
            }
            if (rel.getRelatives() != null) {
                ListUtils.emptyIfNull(rel.getRelatives().getData())
                        .forEach(ri -> {
                            if (StringUtils.isBlank(ri.getId())) {
                                throw new JsonApi4jConstraintViolationException("User id must not be blank", "id");
                            }
                            if (!"users".equalsIgnoreCase(ri.getType())) {
                                throw new JsonApi4jConstraintViolationException("Resource type must be 'users'", "type");
                            }
                            Object meta = ri.getMeta();
                            if (meta instanceof Map<?, ?> m) {
                                Object relationshipTypeObj = m.get(RELATIONSHIP_TYPE_META_KEY);
                                if (relationshipTypeObj instanceof String relationshipType && StringUtils.isNotBlank(relationshipType)) {
                                    try {
                                        RelationshipType.valueOf(relationshipType.toUpperCase());
                                    } catch (Exception ex) {
                                        throw new JsonApi4jConstraintViolationException("Meta 'RelationshipType' object only accepts values: " + Arrays.stream(RelationshipType.values()).map(Enum::name).collect(Collectors.joining(", ")), "meta -> relationshipType");
                                    }
                                }
                            }
                        });
            }
        }
    }

    @Override
    public void validateDelete(JsonApiRequest request) {
        ResourceOperations.super.validateDelete(request);
    }

}
