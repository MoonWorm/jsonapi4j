package io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import io.jsonapi4j.config.OasProperties;
import io.jsonapi4j.operation.OperationsRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

public class CommonOpenApiCustomizer implements OpenApiCustomizer {

    private final OasProperties oasProperties;
    private final OperationsRegistry operationsRegistry;

    public CommonOpenApiCustomizer(OasProperties oasProperties,
                                   OperationsRegistry operationsRegistry) {
        this.oasProperties = oasProperties;
        this.operationsRegistry = operationsRegistry;
    }

    @Override
    public void customise(OpenAPI openApi) {
        new io.jsonapi4j.oas.customizer.CommonOpenApiCustomizer(
                oasProperties,
                operationsRegistry
        ).customise(openApi);
    }

}
