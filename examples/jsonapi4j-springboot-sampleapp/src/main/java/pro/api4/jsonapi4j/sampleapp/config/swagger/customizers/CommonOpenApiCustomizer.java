package pro.api4.jsonapi4j.sampleapp.config.swagger.customizers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

public class CommonOpenApiCustomizer implements OpenApiCustomizer {

    private final OasProperties oasProperties;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public CommonOpenApiCustomizer(OasProperties oasProperties,
                                   DomainRegistry domainRegistry,
                                   OperationsRegistry operationsRegistry) {
        this.oasProperties = oasProperties;
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
    }

    @Override
    public void customise(OpenAPI openApi) {

        new pro.api4.jsonapi4j.plugin.oas.customizer.CommonOpenApiCustomizer(
                oasProperties,
                domainRegistry,
                operationsRegistry
        ).customise(openApi);
    }

}
