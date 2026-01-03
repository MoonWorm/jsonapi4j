package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.operation.ResourceRepository;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.user.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationships;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserResource;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import pro.api4.jsonapi4j.sampleapp.operations.user.validation.UserInputParamsValidator;

import java.util.List;

@RequiredArgsConstructor
@Component
@JsonApiResourceOperation(resource = UserResource.class)
public class UserRepository implements ResourceRepository<UserDbEntity> {

    private final UserDb userDb;
    private final UserInputParamsValidator userValidator;
    private final CountryInputParamsValidator countryValidator;

    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            ),
            parameters = {
                    @Parameter(
                            name = "id",
                            in = OasOperationInfo.In.PATH,
                            description = "User unique identifier",
                            example = "3"
                    )
            }
    )
    @Override
    public UserDbEntity readById(JsonApiRequest request) {
        UserDbEntity userDbEntity = userDb.readById(request.getResourceId());
        if (userDbEntity == null) {
            throwResourceNotFoundException(request);
        }
        return userDbEntity;
    }

    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            ),
            parameters = {
                    @Parameter(
                            name = "filter[id]",
                            description = "Allows to filter users based on id attribute value",
                            example = "3",
                            array = true,
                            required = false
                    )
            }
    )
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

    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            )
    )
    @Override
    public UserDbEntity create(JsonApiRequest request) {
        var payload = request.getSingleResourceDocPayload(UserAttributes.class, UserRelationships.class);
        UserAttributes att = payload.getData().getAttributes();
        var rel = payload.getData().getRelationships();
        if (rel != null && rel.getCitizenships() != null) {
            List<String> countryIds = rel.getCitizenships().getData()
                    .stream()
                    .map(ResourceIdentifierObject::getId)
                    .toList();
            return userDb.createUser(
                    att.getFullName().split("\\s+")[0],
                    att.getFullName().split("\\s+")[1],
                    att.getEmail(),
                    att.getCreditCardNumber(),
                    countryIds
            );
        } else {
            return userDb.createUser(
                    att.getFullName().split("\\s+")[0],
                    att.getFullName().split("\\s+")[1],
                    att.getEmail(),
                    att.getCreditCardNumber()
            );
        }
    }

    @Override
    public void validateCreate(JsonApiRequest request) {
        ResourceRepository.super.validateCreate(request);
        var payload = request.getSingleResourceDocPayload(UserAttributes.class, UserRelationships.class);
        UserAttributes att = payload.getData().getAttributes();
        userValidator.validateFirstName(att.getFullName().split("\\s+")[0]);
        userValidator.validateLastName(att.getFullName().split("\\s+")[1]);
        userValidator.validateEmail(att.getEmail());
        var rel = payload.getData().getRelationships();
        if (rel != null && rel.getCitizenships() != null) {
            rel.getCitizenships().getData()
                    .stream()
                    .map(ResourceIdentifierObject::getId)
                    .forEach(countryValidator::validateCountryId);
        }
    }

}
