package pro.api4.jsonapi4j.sampleapp.operations.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.operation.CreateResourceOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDb;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDbEntity;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserAttributes;
import pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationships;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import pro.api4.jsonapi4j.sampleapp.operations.user.validation.UserInputParamsValidator;

import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;

@RequiredArgsConstructor
@Component
public class CreateUserOperation implements CreateResourceOperation<UserDbEntity> {

    private final UserDb userDb;
    private final UserInputParamsValidator userValidator;
    private final CountryInputParamsValidator countryValidator;

    @Override
    public ResourceType resourceType() {
        return USERS;
    }

    @Override
    public void validate(JsonApiRequest request) {
        CreateResourceOperation.super.validate(request);
        var payload = request.getSingleResourceDocPayload(UserAttributes.class, UserRelationships.class);
        UserAttributes att = payload.getData().getAttributes();
        userValidator.validateFirstName(att.getFirstName());
        userValidator.validateLastName(att.getLastName());
        userValidator.validateEmail(att.getEmail());
        var rel = payload.getData().getRelationships();
        if (rel != null && rel.getCitizenships() != null) {
            rel.getCitizenships().getData()
                    .stream()
                    .map(ResourceIdentifierObject::getId)
                    .forEach(countryValidator::validateCountryId);
        }
    }

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
                    att.getFirstName(),
                    att.getLastName(),
                    att.getEmail(),
                    att.getCreditCardNumber(),
                    countryIds
            );
        } else {
            return userDb.createUser(
                    att.getFirstName(),
                    att.getLastName(),
                    att.getEmail(),
                    att.getCreditCardNumber()
            );
        }
    }

    @Override
    public List<OperationPlugin<?>> plugins() {
        return List.of(
                OperationOasPlugin.builder()
                        .resourceNameSingle("user")
                        .securityConfig(commonSecurityConfig())
                        .build()
        );
    }

}
