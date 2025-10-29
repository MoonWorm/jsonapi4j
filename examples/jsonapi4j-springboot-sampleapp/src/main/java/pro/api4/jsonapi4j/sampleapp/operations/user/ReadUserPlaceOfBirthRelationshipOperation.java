package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.BatchReadToOneRelationshipOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation;
import pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_PLACE_OF_BIRTH;
import static pro.api4.jsonapi4j.sampleapp.domain.user.oas.UserOasSettingsFactory.userIdPathParam;

@RequiredArgsConstructor
@Component
public class ReadUserPlaceOfBirthRelationshipOperation implements BatchReadToOneRelationshipOperation<UserDbEntity, DownstreamCountry> {

    private final RestCountriesFeignClient client;
    private final UserDb userDb;

    @Override
    public DownstreamCountry read(JsonApiRequest request) {
        return ReadCountryByIdOperation.readCountryById(
                userDb.getUserPlaceOfBirth(request.getResourceId()),
                client
        );
    }

    @Override
    public Map<UserDbEntity, DownstreamCountry> readBatches(JsonApiRequest request,
                                                            List<UserDbEntity> users) {
        Set<String> userIds = users.stream().map(UserDbEntity::getId).collect(Collectors.toSet());
        Map<String, UserDbEntity> usersGroupedById = users.stream().collect(Collectors.toMap(UserDbEntity::getId, user -> user));
        Map<String, String> usersPlaceOfBirthMap = userDb.getUsersPlaceOfBirth(userIds);
        List<String> countryIds = usersPlaceOfBirthMap.values()
                .stream()
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
        return usersPlaceOfBirthMap.entrySet().stream().collect(
                CustomCollectors.toMapThatSupportsNullValues(
                        userId -> usersGroupedById.get(userId.getKey()),
                        e -> countries.get(e.getValue())
                )
        );
    }

    @Override
    public RelationshipName relationshipName() {
        return USER_PLACE_OF_BIRTH;
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
