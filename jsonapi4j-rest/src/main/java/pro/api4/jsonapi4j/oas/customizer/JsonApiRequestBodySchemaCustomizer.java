package pro.api4.jsonapi4j.oas.customizer;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.PrimaryAndNestedSchemas;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.plugin.OperationOasPlugin;
import pro.api4.jsonapi4j.plugin.OperationPluginAware;
import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;

import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.generateAllSchemasFromType;
import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.registerSchemaIfNotExists;

@Data
public class JsonApiRequestBodySchemaCustomizer {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public void customise(OpenAPI openApi) {
        registerCreateAndUpdateOperationPayloadSchemas(openApi);
    }

    private void registerCreateAndUpdateOperationPayloadSchemas(OpenAPI openApi) {
        domainRegistry.getResources()
                .stream()
                .sorted()
                .forEach(r -> {
                    ResourceType resourceType = r.resourceType();
                    registerPayloadSchemas(operationsRegistry.getCreateResourceOperation(resourceType, false), openApi);
                    registerPayloadSchemas(operationsRegistry.getUpdateResourceOperation(resourceType, false), openApi);
                    operationsRegistry.getUpdateToOneRelationshipOperations(resourceType).forEach(o -> registerPayloadSchemas(o, openApi));
                    operationsRegistry.getUpdateToManyRelationshipOperationsFor(resourceType).forEach(o -> registerPayloadSchemas(o, openApi));
                });
    }

    private void registerPayloadSchemas(OperationPluginAware operation,
                                        OpenAPI openApi) {
        if (operation != null && operation.getPlugin(OperationOasPlugin.class) != null) {
            Class<?> payloadType = operation.getPlugin(OperationOasPlugin.class).getPayloadType();
            if (payloadType != null) {
                PrimaryAndNestedSchemas schemas = generateAllSchemasFromType(payloadType);
                registerSchemaIfNotExists(schemas.getPrimarySchema(), openApi);
                schemas.getNestedSchemas().forEach(s -> registerSchemaIfNotExists(s, openApi));
            }
        }
    }

}
