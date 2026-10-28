package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.operation.BatchReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.ToManyRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.domain.ResourceRef;
import pro.api4.jsonapi4j.domain.DefaultResourceRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource.COUNTRIES;

@JsonApiRelationshipOperation(
        relationship = UserCitizenshipsRelationship.class
)
@AccessControl(
        authenticated = Authenticated.AUTHENTICATED,
        scopes = @AccessControlScopes(@ScopesGroup({"users.citizenships.read"})),
        ownership = @AccessControlOwnership(ownerIdExtractor = ResourceIdFromUrlPathExtractor.class)
)
@RequiredArgsConstructor
public class UserCitizenshipsOperations implements
        ToManyRelationshipOperations<UserDbEntity, ResourceRef>,
        BatchReadToManyRelationshipOperation<UserDbEntity, ResourceRef> {

    private final UserDb userDb;

    @OasOperationInfo(
            securityConfig = @OasOperationInfo.SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            )
    )
    @Override
    public PaginationAwareResponse<ResourceRef> readMany(JsonApiRequest request) {
        // Citizenship country ids are stored locally on the user — a relationship only emits
        // {type, id}, so we return lightweight refs straight from the local store. The full country
        // is fetched separately by CountryResource only when ?include=citizenships is requested.
        return PaginationAwareResponse.inMemoryCursorAware(
                userDb.getUserCitizenships(request.getResourceId()),
                request.getCursor(),
                2 // set limit to 2
        );
    }

    @Override
    public Map<UserDbEntity, PaginationAwareResponse<ResourceRef>> readBatches(JsonApiRequest request,
                                                                              List<UserDbEntity> users) {
        Set<String> userIds = users.stream().map(UserDbEntity::getId).collect(Collectors.toSet());
        Map<String, UserDbEntity> usersGroupedById = users.stream().collect(Collectors.toMap(UserDbEntity::getId, user -> user));
        Map<String, List<ResourceRef>> usersCitizenshipsMap = userDb.getUsersCitizenships(userIds);
        return usersCitizenshipsMap.entrySet().stream().collect(
                Collectors.toMap(
                        e -> usersGroupedById.get(e.getKey()),
                        e -> PaginationAwareResponse.inMemoryCursorAware(e.getValue())
                )
        );
    }

    @AccessControl(
            authenticated = Authenticated.AUTHENTICATED,
            ownership = @AccessControlOwnership(
                    ownerIdExtractor = ResourceIdFromUrlPathExtractor.class
            )
    )
    @OasOperationInfo(
            securityConfig = @OasOperationInfo.SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            )
    )
    @Override
    public void update(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();

        List<ResourceRef> newCitizenships = ListUtils.emptyIfNull(payload.getData())
                .stream()
                .map(ResourceIdentifierObject::getId)
                .map(String::toUpperCase)
                .distinct()
                .map(id -> DefaultResourceRef.of(COUNTRIES, id))
                .toList();

        userDb.updateUserCitizenships(request.getResourceId(), newCitizenships);
    }

    @OasOperationInfo(
            securityConfig = @OasOperationInfo.SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            )
    )
    @Override
    public void add(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();

        List<ResourceRef> citizenshipsToAdd = ListUtils.emptyIfNull(payload.getData())
                .stream()
                .map(ResourceIdentifierObject::getId)
                .map(String::toUpperCase)
                .distinct()
                .map(id -> DefaultResourceRef.of(COUNTRIES, id))
                .toList();

        userDb.addUserCitizenships(request.getResourceId(), citizenshipsToAdd);
    }

    @OasOperationInfo(
            securityConfig = @OasOperationInfo.SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            )
    )
    @Override
    public void delete(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();

        List<ResourceRef> citizenshipsToRemove = ListUtils.emptyIfNull(payload.getData())
                .stream()
                .map(ResourceIdentifierObject::getId)
                .map(String::toUpperCase)
                .distinct()
                .map(id -> DefaultResourceRef.of(COUNTRIES, id))
                .toList();

        userDb.removeUserCitizenships(request.getResourceId(), citizenshipsToRemove);
    }

    @Override
    public void validateAddToMany(JsonApiRequest request) {
        forRequest(request)
                .toManyRelationshipBody(body -> body
                        .withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId))
                        .withResourceTypeValidator(type -> type.isOneOf(COUNTRIES)))
                .validate();
    }

    @Override
    public void validateDeleteFromToMany(JsonApiRequest request) {
        forRequest(request)
                .toManyRelationshipBody(body -> body
                        .withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId))
                        .withResourceTypeValidator(type -> type.isOneOf(COUNTRIES)))
                .validate();
    }

    @Override
    public void validateUpdateToMany(JsonApiRequest request) {
        forRequest(request)
                .toManyRelationshipBody(body -> body
                        .withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId))
                        .withResourceTypeValidator(type -> type.isOneOf(COUNTRIES)))
                .validate();
    }

}
