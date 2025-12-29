package pro.api4.jsonapi4j.sampleapp.operations.country;

import lombok.RequiredArgsConstructor;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.stereotype.Component;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.Parameter;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo.SecurityConfig;
import pro.api4.jsonapi4j.operation.ReadResourceByIdOperation;
import pro.api4.jsonapi4j.processor.exception.ResourceNotFoundException;
import pro.api4.jsonapi4j.request.JsonApiRequest;
import pro.api4.jsonapi4j.sampleapp.config.datasource.model.country.DownstreamCountry;
import pro.api4.jsonapi4j.sampleapp.config.datasource.RestCountriesFeignClient;
import pro.api4.jsonapi4j.sampleapp.operations.country.validation.CountryInputParamsValidator;

import java.util.Collections;

import static pro.api4.jsonapi4j.sampleapp.domain.SampleAppDomainResourceTypes.COUNTRIES;
import static pro.api4.jsonapi4j.sampleapp.operations.country.ReadMultipleCountriesOperation.readCountriesByIds;

@OasOperationInfo(
        securityConfig = @SecurityConfig(
                clientCredentialsSupported = true,
                pkceSupported = true
        ),
        parameters = {
                @Parameter(
                        name = "id",
                        in = OasOperationInfo.In.PATH,
                        description = "Country unique identifier (ISO 3166)",
                        example = "US"
                )
        }
)
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

}
