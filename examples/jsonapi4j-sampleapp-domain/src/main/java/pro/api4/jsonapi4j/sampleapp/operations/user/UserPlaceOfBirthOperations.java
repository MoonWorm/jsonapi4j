package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.operation.BatchReadToOneRelationshipOperation;
import pro.api4.jsonapi4j.operation.ToOneRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.model.In;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CountryRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserPlaceOfBirthRelationship;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.util.CustomCollectors;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;

import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource.COUNTRIES;

@JsonApiRelationshipOperation(
        relationship = UserPlaceOfBirthRelationship.class
)
@RequiredArgsConstructor
public class UserPlaceOfBirthOperations implements
        ToOneRelationshipOperations<UserDbEntity, CountryRef>,
        BatchReadToOneRelationshipOperation<UserDbEntity, CountryRef> {

    private final UserDb userDb;

    @OasOperationInfo(
            securityConfig = @OasOperationInfo.SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            ),
            parameters = {
                    @OasOperationInfo.Parameter(
                            name = "id",
                            in = In.PATH,
                            description = "User unique identifier",
                            example = "3"
                    )
            }
    )
    @Override
    public CountryRef readOne(JsonApiRequest request) {
        // The place-of-birth country ref lives on the user — a relationship only emits {type, id}, so the
        // lightweight ref off the local store is all that is needed. The full country is materialized by
        // CountryResource only when ?include=placeOfBirth is requested.
        return userDb.getUserPlaceOfBirth(request.getResourceId());
    }

    @Override
    public Map<UserDbEntity, CountryRef> readBatches(JsonApiRequest request,
                                                     List<UserDbEntity> users) {
        Set<String> userIds = users.stream().map(UserDbEntity::getId).collect(Collectors.toSet());
        Map<String, UserDbEntity> usersGroupedById = users.stream().collect(Collectors.toMap(UserDbEntity::getId, user -> user));
        Map<String, CountryRef> usersPlaceOfBirthMap = userDb.getUsersPlaceOfBirth(userIds);
        return usersPlaceOfBirthMap.entrySet().stream().collect(
                CustomCollectors.toMapThatSupportsNullValues(
                        e -> usersGroupedById.get(e.getKey()),
                        Map.Entry::getValue
                )
        );
    }

    @AccessControl(
            authenticated = Authenticated.AUTHENTICATED,
            ownership = @AccessControlOwnership(
                    ownerIdExtractor = ResourceIdFromUrlPathExtractor.class
            )
    )
    @Override
    public void update(JsonApiRequest request) {
        var payload = request.getToOneRelationshipDocPayload();
        if (payload.getData() == null) {
            userDb.deleteUser(request.getResourceId());
        } else {
            userDb.updateUserPlaceOfBirth(
                    request.getResourceId(),
                    new CountryRef(payload.getData().getId())
            );
        }
    }

    @Override
    public void validateUpdateToOne(JsonApiRequest request) {
        forRequest(request)
                .toOneRelationshipBody(body -> body
                        .withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId))
                        .withResourceTypeValidator(type -> type.isOneOf(COUNTRIES)))
                .validate();
    }

}
