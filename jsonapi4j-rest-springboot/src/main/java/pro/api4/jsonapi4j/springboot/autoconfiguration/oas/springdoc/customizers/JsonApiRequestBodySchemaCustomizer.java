package pro.api4.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import pro.api4.jsonapi4j.operation.OperationsRegistry;

public class JsonApiRequestBodySchemaCustomizer implements OpenApiCustomizer {

    private final OperationsRegistry operationsRegistry;

    public JsonApiRequestBodySchemaCustomizer(OperationsRegistry operationsRegistry) {
        this.operationsRegistry = operationsRegistry;
    }

    @Override
    public void customise(OpenAPI openApi) {
        new pro.api4.jsonapi4j.oas.customizer.JsonApiRequestBodySchemaCustomizer(
                operationsRegistry
        ).customise(openApi);
    }


}
