package pro.api4.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
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
        new pro.api4.jsonapi4j.plugin.oas.customizer.JsonApiResponseSchemaCustomizer(
                domainRegistry,
                operationsRegistry
        ).customise(openApi);
    }
}
