package pro.api4.jsonapi4j.sampleapp.operations.country;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation;
import pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.processor.CursorPageableResponse;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient;
import pro.api4.jsonapi4j.sampleapp.operations.CountriesClient.Field;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.domain.country.CountryResource;
import pro.api4.jsonapi4j.sampleapp.domain.country.Region;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.Collections;
import java.util.List;

@JsonApiResourceOperation(resource = CountryResource.class)
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
@RequiredArgsConstructor
public class ReadMultipleCountriesOperation implements ReadMultipleResourcesOperation<DownstreamCountry> {

    public static final String REGION_FILTER_NAME = "region";

    private final CountriesClient client;
    private final CountryInputParamsValidator validator;

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
                    request.getCursor(),
                    2 // page size
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

}
