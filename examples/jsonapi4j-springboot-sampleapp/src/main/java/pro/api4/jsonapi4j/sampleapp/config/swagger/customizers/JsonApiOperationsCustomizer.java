package pro.api4.jsonapi4j.sampleapp.config.swagger.customizers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

public class JsonApiOperationsCustomizer implements OpenApiCustomizer {

    private final String jsonApiRootPath;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final OasProperties oasProperties;

    public JsonApiOperationsCustomizer(String jsonApiRootPath,
                                       DomainRegistry domainRegistry,
                                       OperationsRegistry operationsRegistry,
                                       OasProperties oasProperties) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
        this.oasProperties = oasProperties;
    }

    @Override
    public void customise(OpenAPI openApi) {
        new pro.api4.jsonapi4j.plugin.oas.customizer.JsonApiOperationsCustomizer(
                jsonApiRootPath,
                domainRegistry,
                operationsRegistry,
                oasProperties
        ).customise(openApi);
    }

}
