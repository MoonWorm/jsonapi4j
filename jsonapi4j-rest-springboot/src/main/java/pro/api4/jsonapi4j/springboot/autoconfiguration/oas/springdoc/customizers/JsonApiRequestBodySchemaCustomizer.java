package pro.api4.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
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
        new pro.api4.jsonapi4j.oas.customizer.JsonApiRequestBodySchemaCustomizer(
                domainRegistry,
                operationsRegistry
        ).customise(openApi);
    }


}
