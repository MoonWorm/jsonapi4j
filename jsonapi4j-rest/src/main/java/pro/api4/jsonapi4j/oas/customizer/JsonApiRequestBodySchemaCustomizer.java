package pro.api4.jsonapi4j.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import lombok.Data;
import pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.PrimaryAndNestedSchemas;
import pro.api4.jsonapi4j.operation.plugin.oas.model.OasOperationInfo;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.operation.RegisteredOperation;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.generateAllSchemasFromType;
import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.registerSchemaIfNotExists;

@Data
public class JsonApiRequestBodySchemaCustomizer {

    private final OperationsRegistry operationsRegistry;

    public void customise(OpenAPI openApi) {
        registerCreateAndUpdateOperationPayloadSchemas(openApi);
    }

    private void registerCreateAndUpdateOperationPayloadSchemas(OpenAPI openApi) {
        operationsRegistry.getResourceTypesWithAnyOperationConfigured()
                .stream()
                .sorted()
                .forEach(resourceType -> {
                    registerPayloadSchemas(operationsRegistry.getRegisteredCreateResourceOperation(resourceType, false), openApi);
                    registerPayloadSchemas(operationsRegistry.getRegisteredUpdateResourceOperation(resourceType, false), openApi);
                    operationsRegistry.getRegisteredUpdateToOneRelationshipOperations(resourceType).forEach(o -> registerPayloadSchemas(o, openApi));
                    operationsRegistry.getRegisteredUpdateToManyRelationshipOperationsFor(resourceType).forEach(o -> registerPayloadSchemas(o, openApi));
                });
    }

    private void registerPayloadSchemas(RegisteredOperation<?> operation,
                                        OpenAPI openApi) {
        if (operation != null) {
            if (emptyIfNull(operation.getPluginInfo()).get(JsonApiOasPlugin.NAME) instanceof OasOperationInfo oasOperationInfo) {
                Class<?> payloadType = oasOperationInfo.payloadType();
                if (payloadType != null && !OasOperationInfo.NotApplicable.class.isAssignableFrom(payloadType)) {
                    PrimaryAndNestedSchemas schemas = generateAllSchemasFromType(payloadType);
                    registerSchemaIfNotExists(schemas.getPrimarySchema(), openApi);
                    schemas.getNestedSchemas().forEach(s -> registerSchemaIfNotExists(s, openApi));
                }
            }


        }
    }

}
