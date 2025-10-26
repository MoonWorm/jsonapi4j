package pro.api4.jsonapi4j.sampleapp.operations.currency;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.In;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.Type;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient.Field;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.CURRENCIES;
import static pro.api4.jsonapi4j.sampleapp.domain.currency.oas.CurrencyOasSettingsFactory.CURRENCY_ID_EXAMPLE;

@RequiredArgsConstructor
@Component
public class ReadAllCurrenciesOperation implements ReadMultipleResourcesOperation<DownstreamCurrencyWithCode> {

    private final RestCountriesFeignClient client;

    public static List<DownstreamCurrencyWithCode> readCurrenciesByIds(List<String> ids,
                                                                       RestCountriesFeignClient client) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        List<DownstreamCurrencyWithCode> result = new ArrayList<>();
        for (String id : ids) {
            var responseEntity = client.getByCurrency(
                    id,
                    List.of(Field.currencies)
            );
            if (responseEntity == null || responseEntity.getBody() == null) {
                return Collections.emptyList();
            }
            if (CollectionUtils.isNotEmpty(responseEntity.getBody())) {
                result.add(
                        new DownstreamCurrencyWithCode(
                                id,
                                responseEntity.getBody().get(0).getCurrencies().get(id)
                        )
                );
            }
        }
        return result;
    }

    @Override
    public ResourceType resourceType() {
        return CURRENCIES;
    }

    @Override
    public CursorPageableResponse<DownstreamCurrencyWithCode> readPage(JsonApiRequest request) {
        return CursorPageableResponse.fromItemsNotPageable(
                readCurrenciesByIds(
                        request.getFilters().get(ID_FILTER_NAME),
                        client
                )
        );
    }

    @Override
    public void validate(JsonApiRequest request) {
        ReadMultipleResourcesOperation.super.validate(request);
        if (!request.getFilters().containsKey(ID_FILTER_NAME)) {
            throw new JsonApi4jException(
                    400,
                    DefaultErrorCodes.MISSING_REQUIRED_PARAMETER,
                    "Operation requires ids"
            );
        }
    }

    @Override
    public List<OperationPlugin<?>> plugins() {
        return List.of(
                OperationOasPlugin.builder()
                        .resourceNameSingle("currency")
                        .parameters(
                                List.of(
                                        OperationOasPlugin.ParameterConfig.builder()
                                                .name(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME))
                                                .description("Allows to filter the collection of resources based on " + ID_FILTER_NAME + " attribute value")
                                                .example(CURRENCY_ID_EXAMPLE)
                                                .in(In.QUERY)
                                                .isArray(true)
                                                .type(Type.STRING)
                                                .isRequired(true)
                                                .build()
                                )
                        )
                        .securityConfig(commonSecurityConfig())
                        .build()
        );
    }

}
