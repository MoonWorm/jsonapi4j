package pro.api4.jsonapi4j.sampleapp.operations.user;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.operation.UpdateToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.processor.exception.InvalidPayloadException;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.userdb.UserDb;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.ListUtils;
import org.springframework.stereotype.Component;

import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.USERS;
import static pro.api4.jsonapi4j.sampleapp.domain.country.oas.CountryOasSettingsFactory.countryIdPathParam;
import static pro.api4.jsonapi4j.sampleapp.domain.user.UserRelationshipsRegistry.USER_CITIZENSHIPS;

@RequiredArgsConstructor
@Component
public class UpdateUserCitizenshipsRelationshipOperation implements UpdateToManyRelationshipOperation {

    private final UserDb userDb;
    private final CountryInputParamsValidator countryValidator;

    @Override
    public void update(JsonApiRequest request) {
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();

        List<String> newCountryIds = ListUtils.emptyIfNull(payload.getData())
                .stream()
                .map(ResourceIdentifierObject::getId)
                .toList();

        userDb.updateUserCitizenships(request.getResourceId(), newCountryIds);
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
    public void validate(JsonApiRequest request) {
        UpdateToManyRelationshipOperation.super.validate(request);
        ToManyRelationshipsDoc payload = request.getToManyRelationshipDocPayload();
        if (payload == null) {
            throw new InvalidPayloadException("Payload is required for this operation type but it's missing.");
        }
        payload.getData()
                .stream()
                .map(ResourceIdentifierObject::getId)
                .forEach(countryValidator::validateCountryId);
    }

    @Override
    public List<OperationPlugin<?>> plugins() {
        return List.of(
                OperationOasPlugin.builder()
                        .resourceNameSingle("country")
                        .securityConfig(commonSecurityConfig())
                        .parameters(List.of(countryIdPathParam()))
                        .build()
        );
    }

}
