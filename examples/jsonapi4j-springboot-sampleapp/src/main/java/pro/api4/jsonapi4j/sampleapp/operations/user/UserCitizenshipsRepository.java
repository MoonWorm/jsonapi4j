package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.impl.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.impl.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.operation.ToManyRelationshipBatchAwareRepository;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.exception.InvalidPayloadException;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.*;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_CITIZENSHIPS;

@OasOperationInfo(
        resourceNameSingle = "user",
        securityConfig = @OasOperationInfo.SecurityConfig(
                clientCredentialsSupported = true,
                pkceSupported = true
        ),
        parameters = {
                @OasOperationInfo.Parameter(
                        name = "id",
                        in = OasOperationInfo.In.PATH,
                        description = "User unique identifier",
                        example = "3"
                )
        }
)
@AccessControl(
        authenticated = Authenticated.AUTHENTICATED,
        scopes = @AccessControlScopes(requiredScopes = {"users.citizenships.read"}),
        ownership = @AccessControlOwnership(ownerIdExtractor = ResourceIdFromUrlPathExtractor.class)
)
@RequiredArgsConstructor
@Component
public class UserCitizenshipsRepository implements ToManyRelationshipBatchAwareRepository<UserDbEntity, DownstreamCountry> {

    private final RestCountriesFeignClient client;
    private final UserDb userDb;
    private final CountryInputParamsValidator countryValidator;

    @Override
    public CursorPageableResponse<DownstreamCountry> readMany(JsonApiRequest request) {
        return CursorPageableResponse.fromItemsPageable(
                ReadMultipleCountriesOperation.readCountriesByIds(
                        userDb.getUserCitizenships(request.getResourceId()),
                        client
                ),
                request.getCursor(),
                2 // set limit to 2
        );
    }

    @Override
    public Map<UserDbEntity, CursorPageableResponse<DownstreamCountry>> readBatches(JsonApiRequest request,
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
                        e -> CursorPageableResponse.fromItemsPageable(
                                e.getValue().stream().map(countries::get).filter(Objects::nonNull).toList()
                        )
                )
        );
    }

    @Override
    public void update(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();

        List<String> newCountryIds = ListUtils.emptyIfNull(payload.getData())
                .stream()
                .map(ResourceIdentifierObject::getId)
                .toList();

        userDb.updateUserCitizenships(request.getResourceId(), newCountryIds);
    }

    @Override
    public void validateUpdateToMany(JsonApiRequest request) {
        ToManyRelationshipBatchAwareRepository.super.validateUpdateToMany(request);
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();
        if (payload == null) {
            throw new InvalidPayloadException("Payload is required for this operation type but it's missing.");
        }
        payload.getData()
                .stream()
                .map(ResourceIdentifierObject::getId)
                .forEach(countryValidator::validateCountryId);
    }

    @Override
    public ResourceType resourceType() {
        return USERS;
    }

    @Override
    public RelationshipName relationshipName() {
        return USER_CITIZENSHIPS;
    }
}
