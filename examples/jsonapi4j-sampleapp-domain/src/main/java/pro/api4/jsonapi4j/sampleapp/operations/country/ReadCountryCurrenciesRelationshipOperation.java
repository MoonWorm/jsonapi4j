package pro.api4.jsonapi4j.sampleapp.operations.country;

import lombok.RequiredArgsConstructor;
import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiRelationshipOperation;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.plugin.oas.operation.model.In;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.CurrencyRef;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryCurrenciesRelationship;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;

import java.util.ArrayList;

import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;
import static pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation.readCountryById;

@JsonApiRelationshipOperation(
        relationship = CountryCurrenciesRelationship.class
)
@RequiredArgsConstructor
public class ReadCountryCurrenciesRelationshipOperation implements ReadToManyRelationshipOperation<DownstreamCountry, CurrencyRef> {

    private final CountriesClient client;

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
    @Override
    public PaginationAwareResponse<CurrencyRef> readMany(JsonApiRequest request) {
        return PaginationAwareResponse.inMemoryCursorAware(
                new ArrayList<>(
                        readCountryById(request.getResourceId(), client)
                                .getCurrencies()
                                .keySet()
                                .stream()
                                .map(CurrencyRef::new)
                                .toList()
                ),
                request.getCursor()
        );
    }

    @Override
    public PaginationAwareResponse<CurrencyRef> readManyForResource(JsonApiRequest relationshipRequest,
                                                                    DownstreamCountry resourceDto) {
        // The parent country already holds its currency codes — serve the refs straight off it, no fetch.
        return PaginationAwareResponse.inMemoryCursorAware(
                new ArrayList<>(resourceDto.getCurrencies()
                        .keySet()
                        .stream()
                        .map(CurrencyRef::new)
                        .toList()
                )
        );
    }

    @Override
    public void validate(JsonApiRequest request) {
        forRequest(request)
                .path(path -> path
                        .withResourceIdValidator(id -> id.isNotBlank().satisfies(CountryResource::validateCountryId)))
                .validate();
    }

}
