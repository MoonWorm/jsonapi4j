package pro.api4.jsonapi4j.sampleapp.operations.currency;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.exception.JsonApi4jException;
import pro.api4.jsonapi4j.model.document.error.DefaultErrorCodes;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.operation.ResourceRepository;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient.Field;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.CURRENCIES;

@RequiredArgsConstructor
@Component
public class CurrencyRepository implements ResourceRepository<DownstreamCurrencyWithCode> {

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

    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            ),
            parameters = {
                    @Parameter(
                            name = "filter[id]",
                            description = "Allows to filter currencies based on id attribute value",
                            example = "NOK",
                            array = true,
                            required = false
                    )
            }
    )
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
    public void validateReadMultiple(JsonApiRequest request) {
        ResourceRepository.super.validateReadMultiple(request);
        if (!request.getFilters().containsKey(ID_FILTER_NAME)) {
            throw new JsonApi4jException(
                    400,
                    DefaultErrorCodes.MISSING_REQUIRED_PARAMETER,
                    "Operation requires ids"
            );
        }
    }

    @Override
    public ResourceType resourceType() {
        return CURRENCIES;
    }

}
