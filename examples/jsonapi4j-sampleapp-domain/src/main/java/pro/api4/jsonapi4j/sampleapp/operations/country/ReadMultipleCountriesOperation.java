package pro.api4.jsonapi4j.sampleapp.operations.country;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.response.PaginationAwareResponse;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient.Field;

import pro.api4.jsonapi4j.operation.validation.Validate;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static pro.api4.jsonapi4j.operation.validation.JsonApiRequestValidator.forRequest;

@JsonApiResourceOperation(resource = CountryResource.class)
@RequiredArgsConstructor
public class ReadMultipleCountriesOperation implements ReadMultipleResourcesOperation<DownstreamCountry> {

    public static final String REGION_FILTER_NAME = "region";

    private final CountriesClient client;

    public static List<DownstreamCountry> readCountriesByIds(List<String> ids, CountriesClient client) {
        if (CollectionUtils.isEmpty(ids)) {
            return Collections.emptyList();
        }
        List<DownstreamCountry> downstreamCountries = client.getByCca2(
                ids,
                List.of(Field.cca2, Field.name, Field.region, Field.currencies)
        );
        if (CollectionUtils.isEmpty(downstreamCountries)) {
            return Collections.emptyList();
        }
        return downstreamCountries;
    }

    public static List<DownstreamCountry> readCountriesByRegion(String region,
                                                                CountriesClient client) {
        var resolvedRegion = Region.fromName(region);
        var downstreamCountries = client.getCountriesByRegion(
                resolvedRegion,
                List.of(Field.cca2, Field.name, Field.region, Field.currencies)
        );
        if (CollectionUtils.isEmpty(downstreamCountries)) {
            return Collections.emptyList();
        }
        return downstreamCountries;
    }

    public static List<DownstreamCountry> readAllCountries(CountriesClient client) {
        var downstreamCountries = client.getAllCountries(
                List.of(Field.cca2, Field.name, Field.region, Field.currencies)
        );
        if (CollectionUtils.isEmpty(downstreamCountries)) {
            return Collections.emptyList();
        }
        return downstreamCountries;
    }

    @OasOperationInfo(
            securityConfig = @SecurityConfig(
                    clientCredentialsSupported = true,
                    pkceSupported = true
            ),
            parameters = {
                    @Parameter(
                            name = "filter[id]",
                            description = "Allows to filter countries based on id attribute value",
                            example = "US",
                            array = true,
                            required = false
                    ),
                    @Parameter(
                            name = "filter[region]",
                            description = "Allows to filter countries based on region attribute value",
                            example = "Asia",
                            array = true,
                            required = false
                    )
            }
    )
    @Override
    public PaginationAwareResponse<DownstreamCountry> readPage(JsonApiRequest request) {
        if (request.getFilters().containsKey(ID_FILTER_NAME)) {
            return PaginationAwareResponse.fromItemsNotPageable(
                    readCountriesByIds(
                            request.getFilters().get(ID_FILTER_NAME),
                            client
                    )
            );
        } else if (request.getFilters().containsKey(REGION_FILTER_NAME)) {
            return PaginationAwareResponse.inMemoryCursorAware(
                    readCountriesByRegion(
                            request.getFilters().get(REGION_FILTER_NAME).getFirst(),
                            client
                    ),
                    request.getCursor()
            );
        } else {
            return PaginationAwareResponse.inMemoryCursorAware(
                    readAllCountries(client),
                    request.getCursor(),
                    2 // page size
            );
        }
    }

    @Override
    public void validate(JsonApiRequest request) {
        forRequest(request)
                .parameters(params -> params
                        .withFilterValidator(ID_FILTER_NAME, ids ->
                                ids.ifPresent().allSatisfy(id -> Validate.assertThat(id)
                                        .isNotBlank()
                                        .satisfies(CountryResource::validateCountryId))
                        )
                        .withFilterValidator(REGION_FILTER_NAME, regions ->
                                regions.ifPresent().asList().satisfies(raw -> {
                                    if (!raw.isEmpty()) {
                                        Validate.assertThat(raw.getFirst()).isOneOf(
                                                Arrays.stream(Region.values()).map(Enum::name).toArray(String[]::new));
                                    }
                                })))
                .validate();
    }

}
