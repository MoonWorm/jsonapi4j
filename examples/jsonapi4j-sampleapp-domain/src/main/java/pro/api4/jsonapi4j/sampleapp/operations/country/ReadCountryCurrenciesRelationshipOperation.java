package pro.api4.jsonapi4j.sampleapp.operations.country;

import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.operation.plugin.oas.model.In;
import pro.api4.jsonapi4j.operation.plugin.oas.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.operation.plugin.oas.annotation.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.operation.plugin.oas.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryCurrenciesRelationship;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.ArrayList;

import static pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation.readCountryById;

@JsonApiRelationshipOperation(
        relationship = CountryCurrenciesRelationship.class
)
@OasOperationInfo(
        securityConfig = @SecurityConfig(
                clientCredentialsSupported = true,
                pkceSupported = true
        ),
        parameters = {
                @Parameter(
                        name = "id",
                        in = In.PATH,
                        description = "Country unique identifier (ISO 3166)",
                        example = "US"
                )
        }
)
@RequiredArgsConstructor
public class ReadCountryCurrenciesRelationshipOperation implements ReadToManyRelationshipOperation<DownstreamCountry, DownstreamCurrencyWithCode> {

    private final CountriesClient client;
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
    public void validate(JsonApiRequest request) {
        validator.validateCountryId(request.getResourceId());
    }

}
