package io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import io.jsonapi4j.domain.DomainRegistry;
import io.jsonapi4j.operation.OperationsRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

public class JsonApiResponseSchemaCustomizer implements OpenApiCustomizer {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public JsonApiResponseSchemaCustomizer(DomainRegistry domainRegistry,
                                           OperationsRegistry operationsRegistry) {
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
    }


    @Override
    public void customise(OpenAPI openApi) {
        new io.jsonapi4j.oas.customizer.JsonApiResponseSchemaCustomizer(
                domainRegistry,
                operationsRegistry
        ).customise(openApi);
    }
}
