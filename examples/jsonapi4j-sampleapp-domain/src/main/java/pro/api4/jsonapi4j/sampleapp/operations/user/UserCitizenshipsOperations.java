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
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.model.In;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserCitizenshipsRelationship;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@JsonApiRelationshipOperation(
        relationship = UserCitizenshipsRelationship.class
)
@AccessControl(
        authenticated = Authenticated.AUTHENTICATED,
        scopes = @AccessControlScopes(requiredScopes = {"users.citizenships.read"}),
        ownership = @AccessControlOwnership(ownerIdExtractor = ResourceIdFromUrlPathExtractor.class)
)
@RequiredArgsConstructor
public class UserCitizenshipsOperations implements
        ToManyRelationshipOperations<UserDbEntity, DownstreamCountry>,
        BatchReadToManyRelationshipOperation<UserDbEntity, DownstreamCountry> {

    private final CountriesClient client;
    private final UserDb userDb;
    private final CountryInputParamsValidator countryValidator;

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
    public PaginationAwareResponse<DownstreamCountry> readMany(JsonApiRequest request) {
        return PaginationAwareResponse.inMemoryCursorAware(
                ReadMultipleCountriesOperation.readCountriesByIds(
                        userDb.getUserCitizenships(request.getResourceId()),
                        client
                ),
                request.getCursor(),
                2 // set limit to 2
        );
    }

    @Override
    public Map<UserDbEntity, PaginationAwareResponse<DownstreamCountry>> readBatches(JsonApiRequest request,
                                                                                     List<UserDbEntity> users) {
        Set<String> userIds = users.stream().map(UserDbEntity::getId).collect(Collectors.toSet());
        Map<String, UserDbEntity> usersGroupedById = users.stream().collect(Collectors.toMap(UserDbEntity::getId, user -> user));
        Map<String, List<String>> usersCitizenshipsMap = userDb.getUsersCitizenships(userIds);
        List<String> countryIds = usersCitizenshipsMap.values()
                .stream()
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, DownstreamCountry> countries = ReadMultipleCountriesOperation.readCountriesByIds(countryIds, client)
                .stream()
                .collect(Collectors.groupingBy(
                        DownstreamCountry::getCca2,
                        Collectors.collectingAndThen(
                                Collectors.mapping(c -> c, Collectors.toList()),
                                list -> list.get(0)
                        )
                ));
        return usersCitizenshipsMap.entrySet().stream().collect(
                Collectors.toMap(
                        userId -> usersGroupedById.get(userId.getKey()),
                        e -> PaginationAwareResponse.inMemoryCursorAware(
                                e.getValue().stream().map(countries::get).filter(Objects::nonNull).toList()
                        )
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
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();

        List<String> newCountryIds = ListUtils.emptyIfNull(payload.getData())
                .stream()
                .map(ResourceIdentifierObject::getId)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        userDb.updateUserCitizenships(request.getResourceId(), newCountryIds);
    }

    @Override
    public void add(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();

        List<String> countryIdsToAdd = ListUtils.emptyIfNull(payload.getData())
                .stream()
                .map(ResourceIdentifierObject::getId)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        userDb.addUserCitizenships(request.getResourceId(), countryIdsToAdd);
    }

    @Override
    public void delete(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();

        List<String> countryIdsToRemove = ListUtils.emptyIfNull(payload.getData())
                .stream()
                .map(ResourceIdentifierObject::getId)
                .map(String::toUpperCase)
                .distinct()
                .toList();

        userDb.removeUserCitizenships(request.getResourceId(), countryIdsToRemove);
    }

    @Override
    public void validateAddToMany(JsonApiRequest request) {
        getValidator().validateToManyRelationshipDoc(
                request.getToManyRelationshipDocPayload(),
                countryValidator::validateCountryId,
                resourceType -> getValidator().validateResourceTypeAnyOf(resourceType, Set.of("countries"))
        );
    }

    @Override
    public void validateDeleteFromToMany(JsonApiRequest request) {
        getValidator().validateToManyRelationshipDoc(
                request.getToManyRelationshipDocPayload(),
                countryValidator::validateCountryId,
                resourceType -> getValidator().validateResourceTypeAnyOf(resourceType, Set.of("countries"))
        );
    }

    @Override
    public void validateUpdateToMany(JsonApiRequest request) {
        getValidator().validateToManyRelationshipDoc(
                request.getToManyRelationshipDocPayload(),
                countryValidator::validateCountryId,
                resourceType -> getValidator().validateResourceTypeAnyOf(resourceType, Set.of("countries"))
        );
    }

}
