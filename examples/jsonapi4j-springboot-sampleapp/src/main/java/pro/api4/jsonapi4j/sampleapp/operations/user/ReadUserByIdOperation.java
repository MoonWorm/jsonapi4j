package pro.api4.jsonapi4j.sampleapp.operations.user;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.user.oas.UserOasSettingsFactory.userIdPathParam;

@RequiredArgsConstructor
@Component
public class ReadUserByIdOperation implements ReadResourceByIdOperation<UserDbEntity> {

    private final UserDb userDb;

    @Override
    public ResourceType resourceType() {
        return USERS;
    }

    @Override
    public UserDbEntity readById(JsonApiRequest request) {
        UserDbEntity userDbEntity = userDb.readById(request.getResourceId());
        if (userDbEntity == null) {
            throwResourceNotFoundException(request);
        }
        return userDbEntity;
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
