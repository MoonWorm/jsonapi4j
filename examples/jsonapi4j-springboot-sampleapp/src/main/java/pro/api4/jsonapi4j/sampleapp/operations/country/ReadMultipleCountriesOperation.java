package pro.api4.jsonapi4j.sampleapp.operations.country;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.In;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin.Type;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.FiltersAwareRequest;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient.Field;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.country.oas.CountryOasSettingsFactory.COUNTRY_ID_EXAMPLE;
import static pro.api4.jsonapi4j.sampleapp.domain.country.oas.CountryOasSettingsFactory.REGION_EXAMPLE;

@RequiredArgsConstructor
@Component
public class ReadMultipleCountriesOperation implements ReadMultipleResourcesOperation<DownstreamCountry> {

    public static final String REGION_FILTER_NAME = "region";

    private final RestCountriesFeignClient client;
    private final CountryInputParamsValidator validator;

    public static List<DownstreamCountry> readCountriesByIds(List<String> ids, RestCountriesFeignClient client) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        var responseEntity = client.getByCca2(
                ids,
                List.of(Field.cca2, Field.name, Field.region, Field.currencies)
        );
        if (responseEntity == null || responseEntity.getBody() == null) {
            return Collections.emptyList();
        }
        return responseEntity.getBody();
    }

    public static List<DownstreamCountry> readCountriesByRegion(String region,
                                                                RestCountriesFeignClient client) {
        var resolvedRegion = Region.fromName(region);
        var responseEntity = client.getCountriesByRegion(
                resolvedRegion,
                List.of(Field.cca2, Field.name, Field.region, Field.currencies)
        );
        if (responseEntity == null || responseEntity.getBody() == null) {
            return Collections.emptyList();
        }
        return responseEntity.getBody();
    }

    public static List<DownstreamCountry> readAllCountries(RestCountriesFeignClient client) {
        var responseEntity = client.getAllCountries(
                List.of(Field.cca2, Field.name, Field.region, Field.currencies)
        );
        if (responseEntity == null || responseEntity.getBody() == null) {
            return Collections.emptyList();
        }
        return responseEntity.getBody();
    }

    @Override
    public ResourceType resourceType() {
        return COUNTRIES;
    }

    @Override
    public CursorPageableResponse<DownstreamCountry> readPage(JsonApiRequest request) {
        if (request.getFilters().containsKey(ID_FILTER_NAME)) {
            return CursorPageableResponse.fromItemsNotPageable(
                    readCountriesByIds(
                            request.getFilters().get(ID_FILTER_NAME),
                            client
                    )
            );
        } else if (request.getFilters().containsKey(REGION_FILTER_NAME)) {
            return CursorPageableResponse.fromItemsPageable(
                    readCountriesByRegion(
                            request.getFilters().get(REGION_FILTER_NAME).get(0),
                            client
                    ),
                    request.getCursor()
            );
        } else {
            return CursorPageableResponse.fromItemsPageable(
                    readAllCountries(client),
                    request.getCursor()
            );
        }
    }

    @Override
    public void validate(JsonApiRequest request) {
        if (request.getFilters().containsKey(ID_FILTER_NAME)) {
            validator.validateCountryIds(request.getFilters().get(ID_FILTER_NAME));
        } else if (request.getFilters().containsKey(REGION_FILTER_NAME)) {
            validator.validateRegion(request.getFilters().get(REGION_FILTER_NAME).get(0));
        }
    }

    @Override
    public List<OperationPlugin<?>> plugins() {
        return List.of(
                OperationOasPlugin.builder()
                        .resourceNameSingle("country")
                        .parameters(
                                List.of(
                                        OperationOasPlugin.ParameterConfig.builder()
                                                .name(FiltersAwareRequest.getFilterParam(ID_FILTER_NAME))
                                                .description("Allows to filter the collection of resources based on " + ID_FILTER_NAME + " attribute value")
                                                .example(COUNTRY_ID_EXAMPLE)
                                                .in(In.QUERY)
                                                .isArray(true)
                                                .type(Type.STRING)
                                                .isRequired(false)
                                                .build(),
                                        OperationOasPlugin.ParameterConfig.builder()
                                                .name(FiltersAwareRequest.getFilterParam(REGION_FILTER_NAME))
                                                .description("Allows to filter the collection of resources based on " + REGION_FILTER_NAME + " attribute value")
                                                .example(REGION_EXAMPLE)
                                                .in(In.QUERY)
                                                .type(Type.STRING)
                                                .isRequired(false)
                                                .build()
                                )
                        )
                        .securityConfig(commonSecurityConfig())
                        .build()
        );
    }

}
