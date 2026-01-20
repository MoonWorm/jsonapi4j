package pro.api4.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import pro.api4.jsonapi4j.config.OasProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.Map;

public class JsonApiOperationsCustomizer implements OpenApiCustomizer {

    private final String jsonApiRootPath;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final Map<String, Map<String, OasProperties.ResponseHeader>> customResponseHeaders;

    public JsonApiOperationsCustomizer(String jsonApiRootPath,
                                       DomainRegistry domainRegistry,
                                       OperationsRegistry operationsRegistry,
                                       Map<String, Map<String, OasProperties.ResponseHeader>> customResponseHeaders) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
        this.customResponseHeaders = customResponseHeaders;
    }

    @Override
    public void customise(OpenAPI openApi) {
        new pro.api4.jsonapi4j.plugin.oas.customizer.JsonApiOperationsCustomizer(
                jsonApiRootPath,
                domainRegistry,
                operationsRegistry,
                customResponseHeaders
        ).customise(openApi);
    }

}
