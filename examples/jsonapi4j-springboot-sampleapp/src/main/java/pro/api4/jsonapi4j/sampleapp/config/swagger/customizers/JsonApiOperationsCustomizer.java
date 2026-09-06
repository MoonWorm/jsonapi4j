package pro.api4.jsonapi4j.sampleapp.config.swagger.customizers;

import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.validation.ValidationProperties;
import pro.api4.jsonapi4j.plugin.JsonApi4jPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import java.util.List;

public class JsonApiOperationsCustomizer implements OpenApiCustomizer {

    private final String jsonApiRootPath;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final OasProperties oasProperties;
    private final ValidationProperties validationProperties;
    private final List<JsonApi4jPlugin> plugins;

    public JsonApiOperationsCustomizer(String jsonApiRootPath,
                                       DomainRegistry domainRegistry,
                                       OperationsRegistry operationsRegistry,
                                       OasProperties oasProperties,
                                       ValidationProperties validationProperties,
                                       List<JsonApi4jPlugin> plugins) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
        this.oasProperties = oasProperties;
        this.validationProperties = validationProperties;
        this.plugins = plugins;
    }

    @Override
    public void customise(OpenAPI openApi) {
        new pro.api4.jsonapi4j.plugin.oas.customizer.JsonApiOperationsCustomizer(
                jsonApiRootPath,
                domainRegistry,
                operationsRegistry,
                oasProperties,
                validationProperties,
                plugins
        ).customise(openApi);
    }

}
