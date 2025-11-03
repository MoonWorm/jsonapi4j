package pro.api4.jsonapi4j.sampleapp.operations.user;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.In;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.Type;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.oas.UserOasSettingsFactory.USER_ID_EXAMPLE;

@RequiredArgsConstructor
@Component
public class ReadMultipleUsersOperation implements ReadMultipleResourcesOperation<UserDbEntity> {

    private final UserDb userDb;

    @Override
    public ResourceType resourceType() {
        return USERS;
    }

    @Override
    public CursorPageableResponse<UserDbEntity> readPage(JsonApiRequest request) {
        if (request.getFilters().containsKey(ID_FILTER_NAME)) {
            return CursorPageableResponse.fromItemsNotPageable(
                    userDb.readByIds(request.getFilters().get(ID_FILTER_NAME))
            );
        } else {
            UserDb.DbPage<UserDbEntity> pagedResult = userDb.readAllUsers(request.getCursor());
            return CursorPageableResponse.fromItemsAndCursor(
                    pagedResult.getEntities(),
                    pagedResult.getCursor()
            );
        }
    }

    @Override
    public List<OperationPlugin<?>> plugins() {
        return List.of(
                OperationOasPlugin.builder()
                        .resourceNameSingle("user")
                        .parameters(
                                List.of(
                                        OperationOasPlugin.ParameterConfig.builder()
                                                .name(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME))
                                                .description("Allows to filter the collection of resources based on " + ID_FILTER_NAME + " attribute value")
                                                .example(USER_ID_EXAMPLE)
                                                .in(In.QUERY)
                                                .isArray(true)
                                                .type(Type.STRING)
                                                .isRequired(false)
                                                .build()
                                )
                        )
                        .securityConfig(commonSecurityConfig())
                        .build()
        );
    }

}
