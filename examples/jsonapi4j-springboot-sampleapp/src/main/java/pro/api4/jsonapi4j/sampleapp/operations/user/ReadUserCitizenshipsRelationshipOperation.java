package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.BatchReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_CITIZENSHIPS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.oas.UserOasSettingsFactory.userIdPathParam;

@AccessControl(
        authenticated = Authenticated.AUTHENTICATED,
        scopes = @AccessControlScopes(requiredScopes = {"users.citizenships.read"}),
        ownership = @AccessControlOwnership(ownerIdExtractor = ResourceIdFromUrlPathExtractor.class)
)
@RequiredArgsConstructor
@Component
public class ReadUserCitizenshipsRelationshipOperation implements BatchReadToManyRelationshipOperation<UserDbEntity, DownstreamCountry> {

    private final RestCountriesFeignClient client;
    private final UserDb userDb;

    @Override
    public CursorPageableResponse<DownstreamCountry> read(JsonApiRequest request) {
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
    public RelationshipName relationshipName() {
        return USER_CITIZENSHIPS;
    }

    @Override
    public ResourceType parentResourceType() {
        return USERS;
    }

    @Override
    public List<OperationPlugin<?>> plugins() {
        return List.of(
                OperationOasPlugin.builder()
                        .resourceNameSingle("user")
                        .securityConfig(commonSecurityConfig())
                        .parameters(List.of(userIdPathParam()))
                        .build()
        );
    }

}
