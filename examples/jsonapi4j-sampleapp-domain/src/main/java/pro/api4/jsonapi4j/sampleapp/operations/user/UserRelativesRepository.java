package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.operation.BatchReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.ToManyRelationshipRepository;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.operation.plugin.oas.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.operation.plugin.oas.model.In;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.processor.util.CustomCollectors;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@JsonApiRelationshipOperation(
        relationship = UserRelativesRelationship.class
)
public class UserRelativesRepository implements
        ToManyRelationshipRepository<UserDbEntity, UserRelationshipInfo>,
        BatchReadToManyRelationshipOperation<UserDbEntity, UserRelationshipInfo> {

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
    public CursorPageableResponse<UserRelationshipInfo> readMany(JsonApiRequest request) {
        return CursorPageableResponse.fromItemsPageable(
                userDb.getUserRelatives(request.getResourceId()),
                request.getCursor(),
                2 // page size
        );
    }

    @Override
    public Map<UserDbEntity, CursorPageableResponse<UserRelationshipInfo>> readBatches(JsonApiRequest request,
                                                                                       List<UserDbEntity> users) {
        Set<String> userIds = users.stream().map(UserDbEntity::getId).collect(Collectors.toSet());
        Map<String, UserDbEntity> usersGroupedById = users.stream().collect(Collectors.toMap(UserDbEntity::getId, user -> user));
        Map<String, List<UserRelationshipInfo>> usersRelativesMap = userDb.getUsersRelatives(userIds);
        return usersRelativesMap.entrySet()
                .stream()
                .collect(
                        CustomCollectors.toMapThatSupportsNullValues(
                                e -> usersGroupedById.get(e.getKey()),
                                e -> CursorPageableResponse.fromItemsPageable(e.getValue(), 2)
                        )
                );
    }

}
