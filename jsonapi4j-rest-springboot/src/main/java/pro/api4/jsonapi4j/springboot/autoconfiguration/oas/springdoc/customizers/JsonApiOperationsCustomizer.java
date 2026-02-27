package pro.api4.jsonapi4j.springboot.autoconfiguration.oas.springdoc.customizers;

import pro.api4.jsonapi4j.compatibility.JsonApi4jCompatibilityMode;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import org.springdoc.core.customizers.OpenApiCustomizer;

import java.util.Map;

public class JsonApiOperationsCustomizer implements OpenApiCustomizer {

    private final String jsonApiRootPath;
    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;
    private final Map<String, Map<String, OasProperties.ResponseHeader>> customResponseHeaders;
    private final JsonApi4jCompatibilityMode compatibilityMode;

    public JsonApiOperationsCustomizer(String jsonApiRootPath,
                                       DomainRegistry domainRegistry,
                                       OperationsRegistry operationsRegistry,
                                       Map<String, Map<String, OasProperties.ResponseHeader>> customResponseHeaders) {
        this(jsonApiRootPath, domainRegistry, operationsRegistry, customResponseHeaders, JsonApi4jCompatibilityMode.STRICT);
    }

    public JsonApiOperationsCustomizer(String jsonApiRootPath,
                                       DomainRegistry domainRegistry,
                                       OperationsRegistry operationsRegistry,
                                       Map<String, Map<String, OasProperties.ResponseHeader>> customResponseHeaders,
                                       JsonApi4jCompatibilityMode compatibilityMode) {
        this.jsonApiRootPath = jsonApiRootPath;
        this.domainRegistry = domainRegistry;
        this.operationsRegistry = operationsRegistry;
        this.customResponseHeaders = customResponseHeaders;
        this.compatibilityMode = compatibilityMode == null
                ? JsonApi4jCompatibilityMode.STRICT
                : compatibilityMode;
    }

    @Override
    public void customise(OpenAPI openApi) {
        new pro.api4.jsonapi4j.plugin.oas.customizer.JsonApiOperationsCustomizer(
                jsonApiRootPath,
                domainRegistry,
                operationsRegistry,
                customResponseHeaders,
                compatibilityMode
        ).customise(openApi);
    }

}
