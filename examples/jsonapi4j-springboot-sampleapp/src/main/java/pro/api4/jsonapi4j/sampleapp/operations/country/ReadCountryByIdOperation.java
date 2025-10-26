package pro.api4.jsonapi4j.sampleapp.operations.country;

import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPlugin;
import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.restcountries.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;
import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;

import static pro.api4.jsonapi4j.sampleapp.config.oas.CommonOasSettingsFactory.commonSecurityConfig;
import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.domain.country.oas.CountryOasSettingsFactory.countryIdPathParam;
import static pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation.readCountriesByIds;

@RequiredArgsConstructor
@Component
public class ReadCountryByIdOperation implements ReadResourceByIdOperation<DownstreamCountry> {

    private final RestCountriesFeignClient client;
    private final CountryInputParamsValidator validator;

    public static DownstreamCountry readCountryById(String id, RestCountriesFeignClient client) {
        var result = readCountriesByIds(Collections.singletonList(id), client);
        if (CollectionUtils.isEmpty(result)) {
            throw new ResourceNotFoundException(id, COUNTRIES);
        }
        return result.get(0);
    }

    @Override
    public ResourceType resourceType() {
        return COUNTRIES;
    }

    @Override
    public DownstreamCountry readById(JsonApiRequest request) {
        return readCountryById(request.getResourceId(), client);
    }

    @Override
    public void validate(JsonApiRequest request) {
        validator.validateCountryId(request.getResourceId());
    }

    @Override
    public List<OperationPlugin<?>> plugins() {
        return List.of(
                OperationOasPlugin.builder()
                        .resourceNameSingle("country")
                        .securityConfig(commonSecurityConfig())
                        .parameters(List.of(countryIdPathParam()))
                        .build()
        );
    }
}
