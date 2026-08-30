package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Data;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RegisteredRelationship;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.operation.RegisteredOperation;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceTypes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.PrimaryAndNestedSchemas;
import pro.api4.jsonapi4j.plugin.oas.operation.model.NotApplicable;
import pro.api4.jsonapi4j.plugin.oas.operation.model.OasOperationInfoModel;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject.ID_FIELD;
import static pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject.TYPE_FIELD;
import static pro.api4.jsonapi4j.model.document.data.ResourceObject.ATTRIBUTES_FIELD;
import static pro.api4.jsonapi4j.model.document.data.ResourceObject.LINKS_FIELD;
import static pro.api4.jsonapi4j.model.document.data.ResourceObject.RELATIONSHIPS_FIELD;
import static pro.api4.jsonapi4j.model.document.data.SingleResourceDoc.DATA_FIELD;
import static pro.api4.jsonapi4j.operation.OperationType.ADD_TO_MANY_RELATIONSHIP;
import static pro.api4.jsonapi4j.operation.OperationType.CREATE_RESOURCE;
import static pro.api4.jsonapi4j.operation.OperationType.DELETE_TO_MANY_RELATIONSHIP;
import static pro.api4.jsonapi4j.operation.OperationType.UPDATE_RESOURCE;
import static pro.api4.jsonapi4j.operation.OperationType.UPDATE_TO_MANY_RELATIONSHIPS;
import static pro.api4.jsonapi4j.operation.OperationType.UPDATE_TO_ONE_RELATIONSHIP;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.attributesSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.createRequestDocSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.createResourceSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.requestRelationshipsSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.toManyRelationshipsRequestDocSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.toOneRelationshipRequestDocSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.updateRequestDocSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.updateResourceSchemaName;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.generateAllSchemasFromType;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.generateSchemaFromType;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.registerSchemaIfNotExists;

/**
 * Registers the schemas that write operations accept as request bodies.
 * <p>
 * These are deliberately not the response documents. A request carries neither {@code links} nor {@code included},
 * and {@code id} is optional on create — JSON:API lets the client generate it — while being mandatory on update.
 * Reusing the response schemas would publish a contract that rejects perfectly valid requests, so the request side
 * gets its own {@code …CreateResource} / {@code …UpdateResource} pair and its own linkage-only relationship docs.
 * <p>
 * Resource bodies are named per resource type because they reference that type's attributes; relationship bodies are
 * not, because pure linkage looks the same whatever it points at.
 * <p>
 * Which schemas get registered is derived from the operations that are actually configured, using the same registry
 * queries {@link JsonApiOperationsCustomizer} walks — so every schema an operation references is guaranteed to exist,
 * and none is registered that nothing points at.
 */
@SuppressWarnings({"rawtypes", "unchecked"})
@Data
public class JsonApiRequestBodySchemaCustomizer {

    private static final Set<OperationType> TO_MANY_RELATIONSHIP_WRITES = Set.of(
            UPDATE_TO_MANY_RELATIONSHIPS,
            ADD_TO_MANY_RELATIONSHIP,
            DELETE_TO_MANY_RELATIONSHIP
    );

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public void customise(OpenAPI openApi) {
        OasResourceTypes.resourceTypesWithOperationsExcludingMeta(domainRegistry, operationsRegistry)
                .sorted()
                .forEach(resourceType -> {
                    registerRequestSchemas(resourceType, configuredOperations(resourceType), openApi);
                    registerCustomPayloadSchemas(resourceType, openApi);
                });
    }

    private Set<OperationType> configuredOperations(ResourceType resourceType) {
        Set<OperationType> configured = new LinkedHashSet<>();
        OperationType.getResourceOperationTypes().stream()
                .filter(operationType -> operationsRegistry.isResourceOperationConfigured(resourceType, operationType))
                .forEach(configured::add);
        operationsRegistry.getRelationshipNamesWithAnyOperationConfigured(resourceType)
                .forEach(relationshipName -> OperationType.getAllRelationshipOperationTypes().stream()
                        .filter(operationType -> operationsRegistry.isRelationshipOperationConfigured(resourceType, relationshipName, operationType))
                        .forEach(configured::add));
        return configured;
    }

    private void registerRequestSchemas(ResourceType resourceType,
                                        Set<OperationType> configuredOperations,
                                        OpenAPI openApi) {
        boolean createConfigured = configuredOperations.contains(CREATE_RESOURCE);
        boolean updateConfigured = configuredOperations.contains(UPDATE_RESOURCE);
        boolean resourceWriteConfigured = createConfigured || updateConfigured;

        boolean toOneDocNeeded = configuredOperations.contains(UPDATE_TO_ONE_RELATIONSHIP)
                || (resourceWriteConfigured && !domainRegistry.getToOneRelationships(resourceType).isEmpty());
        boolean toManyDocNeeded = configuredOperations.stream().anyMatch(TO_MANY_RELATIONSHIP_WRITES::contains)
                || (resourceWriteConfigured && !domainRegistry.getToManyRelationships(resourceType).isEmpty());

        if (toOneDocNeeded || toManyDocNeeded) {
            registerSchemaIfNotExists(generateSchemaFromType(ResourceIdentifierObject.class), openApi);
        }
        if (toOneDocNeeded) {
            registerSchemaIfNotExists(toOneRelationshipRequestDocSchema(), openApi);
        }
        if (toManyDocNeeded) {
            registerSchemaIfNotExists(toManyRelationshipsRequestDocSchema(), openApi);
        }
        if (!resourceWriteConfigured) {
            return;
        }

        String relationshipsSchemaName = registerRequestRelationshipsSchema(resourceType, openApi);
        if (createConfigured) {
            registerResourceRequestSchemas(
                    resourceType,
                    createResourceSchemaName(resourceType),
                    createRequestDocSchemaName(resourceType),
                    List.of(TYPE_FIELD),
                    relationshipsSchemaName,
                    openApi
            );
        }
        if (updateConfigured) {
            registerResourceRequestSchemas(
                    resourceType,
                    updateResourceSchemaName(resourceType),
                    updateRequestDocSchemaName(resourceType),
                    List.of(ID_FIELD, TYPE_FIELD),
                    relationshipsSchemaName,
                    openApi
            );
        }
    }

    private void registerResourceRequestSchemas(ResourceType resourceType,
                                                String resourceSchemaName,
                                                String docSchemaName,
                                                List<String> required,
                                                String relationshipsSchemaName,
                                                OpenAPI openApi) {
        registerSchemaIfNotExists(
                requestResourceSchema(resourceType, resourceSchemaName, required, relationshipsSchemaName),
                openApi
        );
        registerSchemaIfNotExists(
                requestDocSchema(docSchemaName, new Schema<>().$ref(resourceSchemaName)),
                openApi
        );
    }

    private String registerRequestRelationshipsSchema(ResourceType resourceType,
                                                      OpenAPI openApi) {
        Map<String, Schema> properties = new LinkedHashMap<>();
        for (RegisteredRelationship<?> relationship : domainRegistry.getToOneRelationships(resourceType)) {
            properties.put(
                    relationship.getRelationshipName().getName(),
                    new Schema<>().$ref(toOneRelationshipRequestDocSchemaName())
            );
        }
        for (RegisteredRelationship<?> relationship : domainRegistry.getToManyRelationships(resourceType)) {
            properties.put(
                    relationship.getRelationshipName().getName(),
                    new Schema<>().$ref(toManyRelationshipsRequestDocSchemaName())
            );
        }
        if (properties.isEmpty()) {
            return null;
        }

        Schema<Object> relationshipsSchema = new Schema<>();
        relationshipsSchema.setName(requestRelationshipsSchemaName(resourceType));
        relationshipsSchema.setType("object");
        relationshipsSchema.setDescription("Relationship linkage to write alongside the resource. Every member is optional — omit one to leave that relationship untouched.");
        relationshipsSchema.setProperties(sortedByKey(properties));
        registerSchemaIfNotExists(relationshipsSchema, openApi);
        return relationshipsSchema.getName();
    }

    private Schema<?> requestResourceSchema(ResourceType resourceType,
                                            String schemaName,
                                            List<String> required,
                                            String relationshipsSchemaName) {
        Schema<?> resourceSchema = generateSchemaFromType(ResourceObject.class);
        resourceSchema.setName(schemaName);
        resourceSchema.getProperties().remove(LINKS_FIELD);

        ((Schema) resourceSchema.getProperties().get(ID_FIELD))
                .example("12345")
                .description(required.contains(ID_FIELD)
                        ? "Resource unique identifier. Must match the id in the URL path"
                        : "Client-generated resource identifier. Optional — omit it to let the server assign one");
        ((Schema) resourceSchema.getProperties().get(TYPE_FIELD))
                .example(resourceType.getType())
                .description("Resource type");

        resourceSchema.setRequired(required);
        resourceSchema.getProperties().put(ATTRIBUTES_FIELD, new Schema<>().$ref(attributesSchemaName(resourceType)));
        if (relationshipsSchemaName == null) {
            resourceSchema.getProperties().remove(RELATIONSHIPS_FIELD);
        } else {
            resourceSchema.getProperties().put(RELATIONSHIPS_FIELD, new Schema<>().$ref(relationshipsSchemaName));
        }
        return resourceSchema;
    }

    private Schema<?> toOneRelationshipRequestDocSchema() {
        Schema<Object> dataSchema = new Schema<>();
        dataSchema.setAllOf(List.of(new Schema<>().$ref(ResourceIdentifierObject.class.getSimpleName())));
        dataSchema.setNullable(true);
        dataSchema.setDescription("Resource linkage, or null to clear the relationship");
        return requestDocSchema(toOneRelationshipRequestDocSchemaName(), dataSchema);
    }

    private Schema<?> toManyRelationshipsRequestDocSchema() {
        ArraySchema dataSchema = new ArraySchema();
        dataSchema.setItems(new Schema<>().$ref(ResourceIdentifierObject.class.getSimpleName()));
        return requestDocSchema(toManyRelationshipsRequestDocSchemaName(), dataSchema);
    }

    private Schema<?> requestDocSchema(String schemaName,
                                       Schema<?> dataSchema) {
        Map<String, Schema> properties = new LinkedHashMap<>();
        properties.put(DATA_FIELD, dataSchema);

        Schema<Object> docSchema = new Schema<>();
        docSchema.setName(schemaName);
        docSchema.setType("object");
        docSchema.setRequired(List.of(DATA_FIELD));
        docSchema.setProperties(properties);
        return docSchema;
    }

    private void registerCustomPayloadSchemas(ResourceType resourceType,
                                              OpenAPI openApi) {
        List<RegisteredOperation<?>> operations = new ArrayList<>();
        operations.add(operationsRegistry.getRegisteredCreateResourceOperation(resourceType, false));
        operations.add(operationsRegistry.getRegisteredUpdateResourceOperation(resourceType, false));
        operations.addAll(operationsRegistry.getRegisteredUpdateToOneRelationshipOperations(resourceType));
        operations.addAll(operationsRegistry.getRegisteredUpdateToManyRelationshipOperationsFor(resourceType));
        operations.stream()
                .filter(Objects::nonNull)
                .forEach(operation -> registerCustomPayloadSchema(operation, openApi));
    }

    private void registerCustomPayloadSchema(RegisteredOperation<?> operation,
                                             OpenAPI openApi) {
        if (emptyIfNull(operation.getOperationMeta().getPluginInfo()).get(JsonApiOasPlugin.NAME)
                instanceof OasOperationInfoModel oasOperationInfo) {
            Class<?> payloadType = oasOperationInfo.getPayloadType();
            if (payloadType != null && !NotApplicable.class.isAssignableFrom(payloadType)) {
                PrimaryAndNestedSchemas schemas = generateAllSchemasFromType(payloadType);
                registerSchemaIfNotExists(schemas.getPrimarySchema(), openApi);
                schemas.getNestedSchemas().forEach(s -> registerSchemaIfNotExists(s, openApi));
            }
        }
    }

    private Map<String, Schema> sortedByKey(Map<String, Schema> properties) {
        Map<String, Schema> sorted = new LinkedHashMap<>();
        properties.keySet().stream().sorted().forEach(name -> sorted.put(name, properties.get(name)));
        return sorted;
    }

}
