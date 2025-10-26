package pro.api4.jsonapi4j.sampleapp.operations.country;

import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.ReadToManyRelationshipOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.country.CountryRelationshipsRegistry.COUNTRY_CURRENCIES;
import static pro.api4.jsonapi4j.sampleapp.domain.currency.oas.CurrencyOasSettingsFactory.currencyIdPathParam;
import static pro.api4.jsonapi4j.sampleapp.operations.country.ReadCountryByIdOperation.readCountryById;

@RequiredArgsConstructor
@Component
public class ReadCountryCurrenciesRelationshipOperation implements ReadToManyRelationshipOperation<DownstreamCountry, DownstreamCurrencyWithCode> {

    private final RestCountriesFeignClient client;
    private final CountryInputParamsValidator validator;

    @Override
    public CursorPageableResponse<DownstreamCurrencyWithCode> read(JsonApiRequest request) {
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
    public CursorPageableResponse<DownstreamCurrencyWithCode> readForResource(JsonApiRequest relationshipRequest,
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
    public ResourceType parentResourceType() {
        return COUNTRIES;
    }

    @Override
    public void validate(JsonApiRequest request) {
        validator.validateCountryId(request.getResourceId());
    }

    @Override
    public List<OperationPlugin<?>> plugins() {
        return List.of(
                OperationOasPlugin.builder()
                        .resourceNameSingle("currency")
                        .securityConfig(commonSecurityConfig())
                        .parameters(List.of(currencyIdPathParam()))
                        .build()
        );
    }

}
