package io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import io.jsonapi4j.config.OasProperties;
import io.jsonapi4j.operation.OperationsRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.Map;

public class JsonApiOperationsCustomizer implements OpenApiCustomizer {

    private final String jsonApiRootPath;
    private final OperationsRegistry operationsRegistry;
    private final Map<String, Map<String, OasProperties.ResponseHeader>> customResponseHeaders;

    public JsonApiOperationsCustomizer(String jsonApiRootPath,
                                       OperationsRegistry operationsRegistry,
                                       Map<String, Map<String, OasProperties.ResponseHeader>> customResponseHeaders) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.operationsRegistry = operationsRegistry;
        this.customResponseHeaders = customResponseHeaders;
    }

    @Override
    public void customise(OpenAPI openApi) {
        new io.jsonapi4j.oas.customizer.JsonApiOperationsCustomizer(
                jsonApiRootPath,
                operationsRegistry,
                customResponseHeaders
        ).customise(openApi);
    }

}
