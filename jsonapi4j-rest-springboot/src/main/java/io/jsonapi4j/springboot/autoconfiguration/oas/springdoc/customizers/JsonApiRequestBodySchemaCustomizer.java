package io.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import io.jsonapi4j.domain.DomainRegistry;
import io.jsonapi4j.operation.OperationsRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

public class JsonApiRequestBodySchemaCustomizer implements OpenApiCustomizer {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public JsonApiRequestBodySchemaCustomizer(DomainRegistry domainRegistry,
                                              OperationsRegistry operationsRegistry) {
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
    }

    @Override
    public void customise(OpenAPI openApi) {
        new io.jsonapi4j.oas.customizer.JsonApiRequestBodySchemaCustomizer(
                domainRegistry,
                operationsRegistry
        ).customise(openApi);
    }


}
