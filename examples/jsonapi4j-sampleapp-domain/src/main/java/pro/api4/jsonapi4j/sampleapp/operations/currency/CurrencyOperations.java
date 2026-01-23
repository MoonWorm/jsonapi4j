package pro.api4.jsonapi4j.sampleapp.operations.currency;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.operation.validation.JsonApi4jDefaultValidator;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient.Field;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCurrencyWithCode;
import pro.api4.jsonapi4j.sampleapp.domain.currency.CurrencyResource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JsonApiResourceOperation(resource = CurrencyResource.class)
@RequiredArgsConstructor
public class CurrencyOperations implements ResourceOperations<DownstreamCurrencyWithCode> {

    private final CountriesClient client;

    public static List<DownstreamCurrencyWithCode> readCurrenciesByIds(List<String> ids,
                                                                       CountriesClient client) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }

        List<DownstreamCurrencyWithCode> result = new ArrayList<>();
        for (String id : ids) {
            List<DownstreamCountry> downstreamCountries = client.getByCurrency(
                    id,
                    List.of(Field.currencies)
            );
            if (CollectionUtils.isEmpty(downstreamCountries)) {
                return Collections.emptyList();
            }
            result.add(
                    new DownstreamCurrencyWithCode(
                            id,
                            downstreamCountries.getFirst().getCurrencies().get(id)
                    )
            );
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
        ResourceOperations.super.validateReadMultiple(request);
        new JsonApi4jDefaultValidator().validateNonNull(request.getFilters().get(ID_FILTER_NAME));
    }

}
