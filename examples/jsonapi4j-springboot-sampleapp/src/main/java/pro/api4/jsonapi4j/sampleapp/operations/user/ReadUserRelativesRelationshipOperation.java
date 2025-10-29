package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.BatchReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_RELATIVES;
import static pro.api4.jsonapi4j.sampleapp.domain.user.oas.UserOasSettingsFactory.userIdPathParam;

@RequiredArgsConstructor
@Component
public class ReadUserRelativesRelationshipOperation implements BatchReadToManyRelationshipOperation<UserDbEntity, UserDbEntity> {

    private final UserDb userDb;

    @Override
    public CursorPageableResponse<UserDbEntity> read(JsonApiRequest request) {
        List<String> relativeIds = userDb.getUserRelatives(request.getResourceId());
        List<UserDbEntity> relatives = userDb.readByIds(relativeIds);
        return CursorPageableResponse.fromItemsPageable(relatives);
    }

    @Override
    public Map<UserDbEntity, CursorPageableResponse<UserDbEntity>> readBatches(JsonApiRequest request,
                                                                               List<UserDbEntity> users) {
        Set<String> userIds = users.stream().map(UserDbEntity::getId).collect(Collectors.toSet());
        Map<String, UserDbEntity> usersGroupedById = users.stream().collect(Collectors.toMap(UserDbEntity::getId, user -> user));
        Map<String, List<String>> usersRelativesMap = userDb.getUsersRelatives(userIds);
        List<String> relativeIds = usersRelativesMap.values()
                .stream()
                .flatMap(Collection::stream)
                .distinct()
                .toList();
        Map<String, UserDbEntity> relatives = userDb.readByIds(relativeIds)
                .stream()
                .collect(Collectors.groupingBy(
                        UserDbEntity::getId,
                        Collectors.collectingAndThen(
                                Collectors.mapping(c -> c, Collectors.toList()),
                                list -> list.get(0)
                        )
                ));
        return usersRelativesMap.entrySet().stream().collect(
                CustomCollectors.toMapThatSupportsNullValues(
                        userId -> usersGroupedById.get(userId.getKey()),
                        e -> CursorPageableResponse.fromItemsPageable(e.getValue().stream().map(relatives::get).toList())
                )
        );
    }

    @Override
    public RelationshipName relationshipName() {
        return USER_RELATIVES;
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
