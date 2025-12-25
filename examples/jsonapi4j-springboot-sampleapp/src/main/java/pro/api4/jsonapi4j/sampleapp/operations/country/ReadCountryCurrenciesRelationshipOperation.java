package pro.api4.jsonapi4j.sampleapp.operations.country;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.ArrayList;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryRelationshipsRegistry.COUNTRY_CURRENCIES;
import static pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation.readCountryById;

@OasOperationInfo(
        securityConfig = @SecurityConfig(
                clientCredentialsSupported = true,
                pkceSupported = true
        ),
        parameters = {
                @Parameter(
                        name = "id",
                        in = OasOperationInfo.In.PATH,
                        description = "Country unique identifier (ISO 3166)",
                        example = "US"
                )
        }
)
@RequiredArgsConstructor
@Component
public class ReadCountryCurrenciesRelationshipOperation implements ReadToManyRelationshipOperation<DownstreamCountry, DownstreamCurrencyWithCode> {

    private final RestCountriesFeignClient client;
    private final CountryInputParamsValidator validator;

    @Override
    public CursorPageableResponse<DownstreamCurrencyWithCode> readMany(JsonApiRequest request) {
        return CursorPageableResponse.fromItemsPageable(
                new ArrayList<>(
                        readCountryById(request.getResourceId(), client)
                                .getCurrencies()
                                .entrySet()
                                .stream()
                                .map(e -> new DownstreamCurrencyWithCode(e.getKey(), e.getValue()))
                                .toList()
                ),
                request.getCursor()
        );
    }

    @Override
    public CursorPageableResponse<DownstreamCurrencyWithCode> readManyForResource(JsonApiRequest relationshipRequest,
                                                                                  DownstreamCountry resourceDto) {
        return CursorPageableResponse.fromItemsPageable(
                new ArrayList<>(resourceDto.getCurrencies()
                        .entrySet()
                        .stream()
                        .map(e -> new DownstreamCurrencyWithCode(e.getKey(), e.getValue()))
                        .toList()
                )
        );
    }

    @Override
    public RelationshipName relationshipName() {
        return COUNTRY_CURRENCIES;
    }

    @Override
    public ResourceType resourceType() {
        return COUNTRIES;
    }

    @Override
    public void validate(JsonApiRequest request) {
        validator.validateCountryId(request.getResourceId());
    }

}
