package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.operation.ToManyRelationshipBatchAwareRepository;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@Component
@JsonApiRelationshipOperation(
        resource = UserResource.class,
        relationship = UserRelativesRelationship.class
)
public class UserRelativesRepository implements ToManyRelationshipBatchAwareRepository<UserDbEntity, UserDbEntity> {

    private final UserDb userDb;

    @OasOperationInfo(
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
    @Override
    public CursorPageableResponse<UserDbEntity> readMany(JsonApiRequest request) {
        List<String> relativeIds = userDb.getUserRelatives(request.getResourceId());
        List<UserDbEntity> relatives = userDb.readByIds(relativeIds);
        return CursorPageableResponse.fromItemsPageable(relatives, request.getCursor(), 2);
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

}
