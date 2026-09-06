package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.apache.commons.collections4.MapUtils;
import org.apache.commons.lang3.StringUtils;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.model.document.data.RelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipObject;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipObject;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.validation.ErrorSources;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.SingleResourceDocValidationBuilder.ToManyRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.SingleResourceDocValidationBuilder.ToOneRelationshipObjectValidationBuilder;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlEntitlements;
import pro.api4.jsonapi4j.plugin.ac.annotation.EntitlementsGroup;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.plugin.oas.operation.model.In;
import pro.api4.jsonapi4j.plugin.oas.operation.model.PaginationStyle;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.request.SortAwareRequest.SortOrder;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CountryRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.AddressRow;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.RelativeRef.RelationshipType;
import pro.api4.jsonapi4j.sampleapp.domain.user.Address;
import pro.api4.jsonapi4j.sampleapp.domain.user.RelativeLinkageMeta;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;
import static pro.api4.jsonapi4j.operation.validation.Validate.assertThat;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.ADMIN;
import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes.EMAIL_FIELD_NAME;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes.FULL_NAME_FIELD_NAME;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship.CITIZENSHIPS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserPlaceOfBirthRelationship.PLACE_OF_BIRTH;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship.RELATIVES;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserResource.USERS;

@RequiredArgsConstructor
@JsonApiResourceOperation(resource = UserResource.class)
public class UserOperations implements ResourceOperations<UserDbEntity> {

    private final UserDb userDb;

    private static final Map<String, Comparator<UserDbEntity>> SORTABLE_ATTRIBUTES = Map.of(
            FULL_NAME_FIELD_NAME, Comparator.comparing(user -> user.getFirstName() + " " + user.getLastName(),
                    String.CASE_INSENSITIVE_ORDER),
            EMAIL_FIELD_NAME, Comparator.comparing(UserDbEntity::getEmail, String.CASE_INSENSITIVE_ORDER)
    );

    public static List<RelativeRef> parseRelations(List<ResourceIdentifierObject> data) {
        List<RelativeRef> relations = new ArrayList<>();
        for (ResourceIdentifierObject ri : ListUtils.emptyIfNull(data)) {
            parseRelationshipType(ri).ifPresent(rt -> relations.add(new RelativeRef(ri.getId(), rt)));
        }
        return relations;
    }

    public static void validateRelationsMeta(Object meta) {
        RelativeLinkageMeta.fromLinkageMeta(meta);
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
            sortableFields = {FULL_NAME_FIELD_NAME, EMAIL_FIELD_NAME},
            pagination = {PaginationStyle.CURSOR, PaginationStyle.LIMIT_OFFSET},
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
            Comparator<UserDbEntity> order = toComparator(request.getSortBy());
            if (StringUtils.isNotBlank(request.getCursor())) {
                UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(request.getCursor(), order);
                return PaginationAwareResponse.cursorAware(
                        pagedResult.getEntities(),
                        pagedResult.getCursor()
                );
            } else if (request.getLimit() != null && request.getOffset() != null) {
                UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(request.getLimit(), request.getOffset(), order);
                return PaginationAwareResponse.limitOffsetAware(
                        pagedResult.getEntities(),
                        pagedResult.getTotalItems()
                );
            } else {
                // fallback to 'null' cursor pagination
                UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(null, order);
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
                att.getCreditCardNumber(),
                toAddressRows(request.getSingleResourceDocPayload().getData().getAttributes())
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
        String userId = payload.getData().getId();

        Map<String, Object> changes = toChangeSet(
                request.getSingleResourceDocPayload().getData().getAttributes());
        if (!changes.isEmpty()) {
            userDb.updateUser(userId, changes);
        }
        updateUserRelationships(userId, payload.getData().getRelationships());
    }

    private static Map<String, Object> toChangeSet(LinkedHashMap<String, Object> attributes) {
        Map<String, Object> changes = new LinkedHashMap<>();
        if (attributes == null) {
            return changes;
        }
        if (attributes.containsKey("fullName")) {
            String[] nameParts = ((String) attributes.get("fullName")).split("\\s+");
            changes.put("firstName", nameParts[0]);
            changes.put("lastName", nameParts[1]);
        }
        for (String attribute : List.of("email", "creditCardNumber")) {
            if (attributes.containsKey(attribute)) {
                changes.put(attribute, attributes.get(attribute));
            }
        }
        if (attributes.containsKey("addresses")) {
            changes.put("addresses", toAddressRows(attributes));
        }
        return changes;
    }

    @SuppressWarnings("unchecked")
    private static List<AddressRow> toAddressRows(LinkedHashMap<String, Object> attributes) {
        if (attributes == null || attributes.get("addresses") == null) {
            return List.of();
        }
        List<AddressRow> rows = new ArrayList<>();
        for (Map<String, Object> address : (List<Map<String, Object>>) attributes.get("addresses")) {
            String city = (String) address.get("city");
            String zip = (String) address.get("zip");
            String doorCode = (String) address.get("doorCode");
            rows.add(doorCode == null ? AddressRow.of(city, zip) : AddressRow.home(city, zip, doorCode));
        }
        return rows;
    }

    private void updateUserRelationships(String userId,
                                         LinkedHashMap<String, RelationshipObject> relationships) {
        if (relationships != null) {
            ToManyRelationshipObject citizenships = (ToManyRelationshipObject) relationships.get(CITIZENSHIPS);
            if (citizenships != null) {
                List<CountryRef> citizenshipRefs = ListUtils.emptyIfNull(citizenships.getData())
                        .stream()
                        .map(ResourceIdentifierObject::getId)
                        .map(CountryRef::new)
                        .toList();
                userDb.updateUserCitizenships(userId, citizenshipRefs);
            }
            ToOneRelationshipObject placeOfBirth = (ToOneRelationshipObject) relationships.get(PLACE_OF_BIRTH);
            if (placeOfBirth != null) {
                userDb.updateUserPlaceOfBirth(
                        userId,
                        placeOfBirth.getData() == null ? null : new CountryRef(placeOfBirth.getData().getId())
                );
            }
            ToManyRelationshipObject relatives = (ToManyRelationshipObject) relationships.get(RELATIVES);
            if (relatives != null) {
                userDb.updateUserRelatives(userId, parseRelations(relatives.getData()));
            }
        }
    }

    @AccessControl(entitlements = @AccessControlEntitlements(@EntitlementsGroup(ADMIN)))
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
    public void validateReadMultiple(JsonApiRequest request) {
        forRequest(request)
                .parameters(params -> params
                        .withSortValidator(sortBy -> sortBy.ifPresent().satisfies(requested ->
                                requested.keySet().forEach(attribute -> assertThat(attribute)
                                        .withErrorCode(DefaultErrorCodes.INVALID_ENUM_VALUE)
                                        .isOneOf(SORTABLE_ATTRIBUTES.keySet().toArray(new String[0]))))))
                .validate();
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
                            att.satisfies(UserOperations::assertAddressesNameACity);
                        })
                        .withToManyRelationship(CITIZENSHIPS, this::citizenshipsValidator)
                        .withToOneRelationship(PLACE_OF_BIRTH, this::placeOfBirthValidator)
                        .withToManyRelationship(RELATIVES, this::relativesValidator))
                .validate();
    }

    private static void assertAddressesNameACity(UserAttributes attributes) {
        List<Address> addresses = attributes.getAddresses();
        if (addresses == null) {
            return;
        }
        for (int i = 0; i < addresses.size(); i++) {
            Address address = addresses.get(i);
            assertThat(address == null ? null : address.getCity())
                    .withSource(ErrorSources.pointer().data().attributes("addresses/" + i + "/city"))
                    .isNotBlank();
        }
    }

    @SuppressWarnings("unchecked")
    private static void assertAddressesNameACity(LinkedHashMap<String, Object> attributes) {
        Object addresses = attributes.get("addresses");
        if (!(addresses instanceof List<?> elements)) {
            return;
        }
        for (int i = 0; i < elements.size(); i++) {
            Map<String, Object> address = (Map<String, Object>) elements.get(i);
            assertThat(address == null ? null : (String) address.get("city"))
                    .withSource(ErrorSources.pointer().data().attributes("addresses/" + i + "/city"))
                    .isNotBlank();
        }
    }

    private static void assertRequiredAttributesAreNotCleared(LinkedHashMap<String, Object> attributes) {
        for (String required : List.of("fullName", "email")) {
            if (attributes.containsKey(required)) {
                assertThat(attributes.get(required))
                        .withSource(ErrorSources.pointer().data().attributes(required))
                        .isNotNull();
            }
        }
    }

    @Override
    public void validateUpdate(JsonApiRequest request) {
        forRequest(request)
                .singleResourceBody(body -> body
                        .withResourceIdValidator(id -> id.exists(resourceId -> userDb.readById(resourceId) != null))
                        .withResourceTypeValidator(type -> type.isOneOf(USERS))
                        .withAttributesValidator(v -> {
                            v.ifPresent();
                            v.field("fullName", att -> (String) att.get("fullName")).ifPresent().asString()
                                    .satisfies(name -> {
                                        assertThat(name.split("\\s+")[0]).isNotBlank().hasLengthBetween(1, 64);
                                        assertThat(name.split("\\s+")[1]).isNotBlank().hasLengthBetween(1, 64);
                                    });
                            v.field("email", att -> (String) att.get("email")).ifPresent().asString().isEmail();
                            v.satisfies(UserOperations::assertRequiredAttributesAreNotCleared);
                            v.satisfies(UserOperations::assertAddressesNameACity);
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

    private static Optional<RelationshipType> parseRelationshipType(ResourceIdentifierObject ri) {
        return RelativeLinkageMeta.fromLinkageMeta(ri.getMeta()).map(RelativeLinkageMeta::relationshipType);
    }

    private static Comparator<UserDbEntity> toComparator(Map<String, SortOrder> sortBy) {
        Comparator<UserDbEntity> result = null;
        for (Map.Entry<String, SortOrder> entry : MapUtils.emptyIfNull(sortBy).entrySet()) {
            Comparator<UserDbEntity> next = SORTABLE_ATTRIBUTES.get(entry.getKey());
            if (next == null) {
                continue;
            }
            if (entry.getValue() == SortOrder.DESC) {
                next = next.reversed();
            }
            result = result == null ? next : result.thenComparing(next);
        }
        return result;
    }

}
