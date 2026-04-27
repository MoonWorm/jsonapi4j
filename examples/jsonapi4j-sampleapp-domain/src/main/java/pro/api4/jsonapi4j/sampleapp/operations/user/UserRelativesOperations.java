package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.operation.BatchReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.ToManyRelationshipOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlOwnership;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.model.In;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserRelationshipInfo.RelationshipType;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelativesRelationship;
import pro.api4.jsonapi4j.sampleapp.operations.UserDb;
import pro.api4.jsonapi4j.util.CustomCollectors;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@RequiredArgsConstructor
@JsonApiRelationshipOperation(
        relationship = UserRelativesRelationship.class
)
public class UserRelativesOperations implements
        ToManyRelationshipOperations<UserDbEntity, UserRelationshipInfo>,
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
    public PaginationAwareResponse<UserRelationshipInfo> readMany(JsonApiRequest request) {
        return PaginationAwareResponse.inMemoryCursorAware(
                userDb.getUserRelatives(request.getResourceId()),
                request.getCursor(),
                2 // page size
        );
    }

    @Override
    public Map<UserDbEntity, PaginationAwareResponse<UserRelationshipInfo>> readBatches(JsonApiRequest request,
                                                                                        List<UserDbEntity> users) {
        Set<String> userIds = users.stream().map(UserDbEntity::getId).collect(Collectors.toSet());
        Map<String, UserDbEntity> usersGroupedById = users.stream().collect(Collectors.toMap(UserDbEntity::getId, user -> user));
        Map<String, List<UserRelationshipInfo>> usersRelativesMap = userDb.getUsersRelatives(userIds);
        return usersRelativesMap.entrySet()
                .stream()
                .collect(
                        CustomCollectors.toMapThatSupportsNullValues(
                                e -> usersGroupedById.get(e.getKey()),
                                e -> PaginationAwareResponse.inMemoryCursorAware(e.getValue(), 2)
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
        Map<String, RelationshipType> newRelations = UserOperations.parseRelations(payload.getData());
        userDb.updateUserRelatives(request.getResourceId(), newRelations);
    }

    @Override
    public void add(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();
        Map<String, RelationshipType> newRelations = UserOperations.parseRelations(payload.getData());
        userDb.addUserRelatives(request.getResourceId(), newRelations);
    }

    @Override
    public void delete(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();
        Set<String> relativeIdsToRemove = payload.getData()
                .stream()
                .map(rio -> rio.getId())
                .collect(Collectors.toSet());
        userDb.removeUserRelatives(request.getResourceId(), relativeIdsToRemove);
    }

    @Override
    public void validateAddToMany(JsonApiRequest request) {
        getValidator().validateToManyRelationshipDoc(
                request.getToManyRelationshipDocPayload(),
                resourceId -> {
                    getValidator().validateNonBlank(resourceId, "data -> resourceId");
                    getValidator().validateResourceId(resourceId);
                    if (userDb.readById(resourceId) == null) {
                        throwResourceNotFoundException(request);
                    }
                },
                resourceType -> getValidator().validateResourceTypeAnyOf(resourceType, Set.of("users")),
                UserOperations::validateRelationsMeta
        );
    }

    @Override
    public void validateDeleteFromToMany(JsonApiRequest request) {
        getValidator().validateToManyRelationshipDoc(
                request.getToManyRelationshipDocPayload(),
                getValidator()::validateResourceId,
                resourceType -> getValidator().validateResourceTypeAnyOf(resourceType, Set.of("users"))
        );
    }

    @Override
    public void validateUpdateToMany(JsonApiRequest request) {
        getValidator().validateToManyRelationshipDoc(
                request.getToManyRelationshipDocPayload(),
                resourceId -> {
                    getValidator().validateNonBlank(resourceId, "data -> resourceId");
                    getValidator().validateResourceId(resourceId);
                    if (userDb.readById(resourceId) == null) {
                        throwResourceNotFoundException(request);
                    }
                },
                resourceType -> getValidator().validateResourceTypeAnyOf(resourceType, Set.of("users")),
                UserOperations::validateRelationsMeta
        );
    }
}
