package pro.api4.jsonapi4j.oas.customizer;

import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.domain.RelationshipName;
import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.ToManyRelationship;
import pro.api4.jsonapi4j.domain.ToOneRelationship;
import pro.api4.jsonapi4j.domain.plugin.oas.RelationshipOasPlugin;
import pro.api4.jsonapi4j.domain.plugin.oas.ResourceOasPlugin;
import pro.api4.jsonapi4j.model.document.data.MultipleResourcesDoc;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.ResourceObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;
import pro.api4.jsonapi4j.model.document.data.ToManyRelationshipsDoc;
import pro.api4.jsonapi4j.model.document.data.ToOneRelationshipDoc;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.PrimaryAndNestedSchemas;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.attributesSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.customResourceIdentifierMetaSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.customResourceIdentifierSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.customToManyRelationshipDocSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.customToOneRelationshipDocSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.errorsDocSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.multipleResourcesDocSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.relationshipsSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.resourceSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.singleResourceDocSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.toManyRelationshipsDocSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.toOneRelationshipDocSchemaName;
import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.generateAllSchemasFromType;
import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.generateSchemaFromType;
import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.registerSchemaIfNotExists;

@SuppressWarnings("ALL")
@Data
public class JsonApiResponseSchemaCustomizer {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public void customise(OpenAPI openApi) {
        registerErrorDocSchemas(openApi);
        registerDataDocsSchemas(openApi);
    }

    private void registerErrorDocSchemas(OpenAPI openApi) {
        PrimaryAndNestedSchemas errorDocSchemas = generateAllSchemasFromType(ErrorsDoc.class);
        errorDocSchemas.getPrimarySchema().setName(errorsDocSchemaName());
        registerSchemaIfNotExists(errorDocSchemas.getPrimarySchema(), openApi);
        errorDocSchemas.getNestedSchemas().forEach(s -> registerSchemaIfNotExists(s, openApi));
    }

    private void registerDataDocsSchemas(OpenAPI openApi) {
        domainRegistry.getResources()
                .stream()
                .sorted()
                .flatMap(resourceConfig -> generateSchemasForResource(resourceConfig).stream())
                .forEach(s -> registerSchemaIfNotExists(s, openApi));
    }

    private List<Schema> generateSchemasForResource(Resource<?> resourceConfig) {

        PrimaryAndNestedSchemas attAndNestedSchemas = generateJsonApiAttributesSchema(resourceConfig);

        PrimaryAndNestedSchemas defaultToManyRelationshipsDocSchemas = generateAllSchemasFromType(ToManyRelationshipsDoc.class);
        PrimaryAndNestedSchemas defaultToOneRelationshipDocSchemas = generateAllSchemasFromType(ToOneRelationshipDoc.class);
        Optional<PrimaryAndNestedSchemas> relationshipsSchemas = generateJsonApiRelationshipsSchema(
                resourceConfig,
                defaultToManyRelationshipsDocSchemas.getPrimarySchema().getName(),
                defaultToOneRelationshipDocSchemas.getPrimarySchema().getName()
        );

        PrimaryAndNestedSchemas resourceSchemas = generateResourceSchema(
                resourceConfig,
                attAndNestedSchemas.getPrimarySchema(),
                relationshipsSchemas.map(PrimaryAndNestedSchemas::getPrimarySchema)
        );

        List<Schema> schemas = new ArrayList<>();
        schemas.add(attAndNestedSchemas.getPrimarySchema());
        schemas.addAll(attAndNestedSchemas.getNestedSchemas());
        relationshipsSchemas.ifPresent(rs -> {
            schemas.add(defaultToManyRelationshipsDocSchemas.getPrimarySchema());
            schemas.addAll(defaultToManyRelationshipsDocSchemas.getNestedSchemas());
            schemas.add(defaultToOneRelationshipDocSchemas.getPrimarySchema());
            schemas.addAll(defaultToOneRelationshipDocSchemas.getNestedSchemas());

            schemas.add(rs.getPrimarySchema());
            schemas.addAll(rs.getNestedSchemas());
        });
        schemas.add(resourceSchemas.getPrimarySchema());
        schemas.addAll(resourceSchemas.getNestedSchemas());

        ResourceType resourceType = resourceConfig.resourceType();
        if (operationsRegistry.isAnyResourceOperationConfigured(resourceType)) {
            PrimaryAndNestedSchemas singleResourceDocSchemas = generateSingleResourceDocSchema(
                    resourceConfig,
                    resourceSchemas.getPrimarySchema()
            );
            PrimaryAndNestedSchemas multipleResourcesDocSchemas = generateMultipleResourcesDocSchema(
                    resourceConfig,
                    resourceSchemas.getPrimarySchema()
            );

            schemas.add(singleResourceDocSchemas.getPrimarySchema());
            schemas.addAll(singleResourceDocSchemas.getNestedSchemas());
            schemas.add(multipleResourcesDocSchemas.getPrimarySchema());
            schemas.addAll(multipleResourcesDocSchemas.getNestedSchemas());
        }

        Schema<?> resourceIdentifierSchema = generateSchemaFromType(ResourceIdentifierObject.class);
        boolean isAnyToManyRelationshipsConfigured
                = operationsRegistry.isAnyToManyRelationshipOperationConfigured(resourceType);
        boolean isAnyToOneRelationshipsConfigured
                = operationsRegistry.isAnyToOneRelationshipOperationConfigured(resourceType);
        if (isAnyToManyRelationshipsConfigured || isAnyToOneRelationshipsConfigured) {
            schemas.add(resourceIdentifierSchema);
        }
        if (isAnyToManyRelationshipsConfigured) {
            PrimaryAndNestedSchemas toManyRelationshipsDocSchemas = generateToManyRelationshipDocSchema(
                    resourceConfig,
                    resourceIdentifierSchema.getName()
            );
            schemas.add(toManyRelationshipsDocSchemas.getPrimarySchema());
            schemas.addAll(toManyRelationshipsDocSchemas.getNestedSchemas());
        }
        if (isAnyToOneRelationshipsConfigured) {
            PrimaryAndNestedSchemas toOneRelationshipDocSchemas = generateToOneRelationshipDocSchema(
                    resourceConfig,
                    resourceIdentifierSchema.getName()
            );
            schemas.add(toOneRelationshipDocSchemas.getPrimarySchema());
            schemas.addAll(toOneRelationshipDocSchemas.getNestedSchemas());
        }
        return schemas;
    }

    private PrimaryAndNestedSchemas generateJsonApiAttributesSchema(Resource<?> resourceConfig) {
        PrimaryAndNestedSchemas result;
        if (resourceConfig.getPlugin(ResourceOasPlugin.class) != null) {
            Class<?> attClass = resourceConfig.getPlugin(ResourceOasPlugin.class).getAttributes();
            result = generateAllSchemasFromType(attClass);
        } else {
            result = new PrimaryAndNestedSchemas(new Schema(), Collections.emptyList());
        }
        result.getPrimarySchema().setName(attributesSchemaName(resourceConfig.resourceType()));
        return result;
    }

    private Optional<PrimaryAndNestedSchemas> generateJsonApiRelationshipsSchema(Resource<?> resourceConfig, String toManyRelationshipsDocSchemaName, String toOneRelationshipDocSchemaName) {
        Schema relationshipsSchema = new Schema<>();
        List<Schema> nestedSchemas = new ArrayList<>();
        PrimaryAndNestedSchemas result = new PrimaryAndNestedSchemas(relationshipsSchema, nestedSchemas);

        List<String> relationshipNames = new ArrayList<>();

        Map<String, Schema> relationshipsSchemaProperties = new HashMap<>();
        for (ToManyRelationship<?, ?> relationshipConfig
                : domainRegistry.getToManyRelationships(resourceConfig.resourceType())) {
            ResourceType resourceType = relationshipConfig.resourceType();
            RelationshipName relationship = relationshipConfig.relationshipName();
            relationshipNames.add(relationship.getName());
            RelationshipOasPlugin relationshipOasExtension = relationshipConfig.getPlugin(RelationshipOasPlugin.class);
            if (relationshipOasExtension != null && relationshipOasExtension.getResourceLinkageMetaType() != null) {
                PrimaryAndNestedSchemas customToManyRelationshipsDocSchema = generateCustomToManyRelationshipsDocSchema(
                        resourceType,
                        relationship,
                        relationshipOasExtension.getResourceLinkageMetaType()
                );
                result.addToNested(customToManyRelationshipsDocSchema);
                relationshipsSchemaProperties.put(
                        relationship.getName(),
                        new Schema<>().$ref(customToManyRelationshipsDocSchema.getPrimarySchema().getName())
                );
            } else {
                relationshipsSchemaProperties.put(
                        relationship.getName(),
                        new Schema<>().$ref(toManyRelationshipsDocSchemaName)
                );
            }
        }
        for (ToOneRelationship<?, ?> relationshipConfig
                : domainRegistry.getToOneRelationships(resourceConfig.resourceType())) {
            ResourceType resourceType = relationshipConfig.resourceType();
            RelationshipName relationship = relationshipConfig.relationshipName();
            relationshipNames.add(relationship.getName());
            RelationshipOasPlugin relationshipOasExtension = relationshipConfig.getPlugin(RelationshipOasPlugin.class);
            if (relationshipOasExtension != null && relationshipOasExtension.getResourceLinkageMetaType() != null) {
                PrimaryAndNestedSchemas customToOneRelationshipDocSchema = generateCustomToOneRelationshipDocSchema(
                        resourceType,
                        relationship,
                        relationshipOasExtension.getResourceLinkageMetaType()
                );
                relationshipsSchemaProperties.put(
                        relationship.getName(),
                        new Schema<>().$ref(customToOneRelationshipDocSchema.getPrimarySchema().getName())
                );
            } else {
                relationshipsSchemaProperties.put(
                        relationship.getName(),
                        new Schema<>().$ref(toOneRelationshipDocSchemaName)
                );
            }
        }
        if (relationshipNames.isEmpty()) {
            return Optional.empty();
        }
        relationshipNames = relationshipNames.stream().sorted().toList();
        relationshipsSchema.setRequired(relationshipNames);

        relationshipsSchema.setProperties(relationshipsSchemaProperties);
        relationshipsSchema.setName(relationshipsSchemaName(resourceConfig.resourceType()));

        return Optional.of(result);
    }

    private PrimaryAndNestedSchemas generateCustomToManyRelationshipsDocSchema(
            ResourceType resourceType,
            RelationshipName relationshipName,
            Class<?> dataItemMetaClass
    ) {
        Schema<?> customToManyRelationshipsDocSchema = generateSchemaFromType(ToManyRelationshipsDoc.class);
        customToManyRelationshipsDocSchema.setName(customToManyRelationshipDocSchemaName(resourceType, relationshipName));

        Schema<?> customResourceIdentifierSchema = generateSchemaFromType(ResourceIdentifierObject.class);
        customResourceIdentifierSchema.setName(customResourceIdentifierSchemaName(resourceType, relationshipName));

        PrimaryAndNestedSchemas customResourceIdentifierMetaSchemas = generateAllSchemasFromType(dataItemMetaClass);
        customResourceIdentifierMetaSchemas.getPrimarySchema().setName(customResourceIdentifierMetaSchemaName(resourceType, relationshipName));

        customResourceIdentifierSchema.getProperties().put("meta", new Schema().$ref(customResourceIdentifierMetaSchemas.getPrimarySchema().getName()));

        ArraySchema dataSchema = (ArraySchema) customToManyRelationshipsDocSchema.getProperties().get("data");
        dataSchema.setItems(new Schema<>().$ref(customResourceIdentifierSchema.getName()));

        List<Schema> nested = new ArrayList<>();
        nested.add(customResourceIdentifierSchema);
        nested.add(customResourceIdentifierMetaSchemas.getPrimarySchema());
        nested.addAll(customResourceIdentifierMetaSchemas.getNestedSchemas());
        return new PrimaryAndNestedSchemas(customToManyRelationshipsDocSchema, nested);
    }

    private PrimaryAndNestedSchemas generateCustomToOneRelationshipDocSchema(
            ResourceType resourceType,
            RelationshipName relationshipName,
            Class<?> dataItemMetaClass
    ) {
        Schema<?> customToOneRelationshipDocSchema = generateSchemaFromType(ToOneRelationshipDoc.class);
        customToOneRelationshipDocSchema.setName(customToOneRelationshipDocSchemaName(resourceType, relationshipName));

        Schema<?> customResourceIdentifierSchema = generateSchemaFromType(ResourceIdentifierObject.class);
        customResourceIdentifierSchema.setName(customResourceIdentifierSchemaName(resourceType, relationshipName));

        PrimaryAndNestedSchemas customResourceIdentifierMetaSchemas = generateAllSchemasFromType(dataItemMetaClass);
        customResourceIdentifierMetaSchemas.getPrimarySchema().setName(customResourceIdentifierMetaSchemaName(resourceType, relationshipName));

        customResourceIdentifierSchema.getProperties().put("meta", new Schema().$ref(customResourceIdentifierMetaSchemas.getPrimarySchema().getName()));

        customToOneRelationshipDocSchema.getProperties().put("data", new Schema<>().$ref(customResourceIdentifierSchema.getName()));

        List<Schema> nested = new ArrayList<>();
        nested.add(customResourceIdentifierSchema);
        nested.add(customResourceIdentifierMetaSchemas.getPrimarySchema());
        nested.addAll(customResourceIdentifierMetaSchemas.getNestedSchemas());
        return new PrimaryAndNestedSchemas(customToOneRelationshipDocSchema, nested);
    }

    private PrimaryAndNestedSchemas generateResourceSchema(Resource<?> resourceConfig,
                                                                               Schema<?> attributesSchema,
                                                                               Optional<Schema<?>> relationshipsSchema) {
        PrimaryAndNestedSchemas resourceSchema = generateAllSchemasFromType(ResourceObject.class);
        resourceSchema.getPrimarySchema().setName(resourceSchemaName(resourceConfig.resourceType()));
        resourceSchema.getPrimarySchema().getProperties().put("attributes", new Schema<>().$ref(attributesSchema.getName()));
        relationshipsSchema.ifPresentOrElse(rs -> {
            resourceSchema.getPrimarySchema().getProperties().put("relationships", new Schema<>().$ref(rs.getName()));
        }, () -> resourceSchema.getPrimarySchema().getProperties().remove("relationships"));
        return resourceSchema;
    }

    private PrimaryAndNestedSchemas generateSingleResourceDocSchema(
            Resource<?> resourceConfig, Schema<?> resourceSchema
    ) {
        PrimaryAndNestedSchemas singleResourceDocSchema = generateAllSchemasFromType(SingleResourceDoc.class);
        singleResourceDocSchema.getPrimarySchema().getProperties().put("data", new Schema<>().$ref(resourceSchema.getName()));
        generateIncludedSchema(resourceConfig.resourceType(), false).ifPresent(s -> singleResourceDocSchema.getPrimarySchema().getProperties().put("included", s));
        singleResourceDocSchema.getPrimarySchema().setName(singleResourceDocSchemaName(resourceConfig.resourceType()));
        return singleResourceDocSchema;
    }

    private PrimaryAndNestedSchemas generateMultipleResourcesDocSchema(
            Resource<?> resourceConfig, Schema<?> resourceSchema
    ) {
        PrimaryAndNestedSchemas multipleResourceSchema = generateAllSchemasFromType(MultipleResourcesDoc.class);
        multipleResourceSchema.getPrimarySchema().getProperties().put("data", new ArraySchema().items(new Schema<>().$ref(resourceSchema.getName())));
        generateIncludedSchema(resourceConfig.resourceType(), false).ifPresent(s -> multipleResourceSchema.getPrimarySchema().getProperties().put("included", s));
        multipleResourceSchema.getPrimarySchema().setName(multipleResourcesDocSchemaName(resourceConfig.resourceType()));
        return multipleResourceSchema;
    }

    private PrimaryAndNestedSchemas generateToManyRelationshipDocSchema(
            Resource<?> resourceConfig, String resourceIdentifierSchemaName
    ) {
        PrimaryAndNestedSchemas toManyRelationshipsDocSchema = generateAllSchemasFromType(ToManyRelationshipsDoc.class);
        toManyRelationshipsDocSchema.getPrimarySchema().getProperties().put("data", new ArraySchema().items(new Schema<>().$ref(resourceIdentifierSchemaName)));
        generateIncludedSchema(resourceConfig.resourceType(), true).ifPresent(s -> toManyRelationshipsDocSchema.getPrimarySchema().getProperties().put("included", s));
        toManyRelationshipsDocSchema.getPrimarySchema().setName(toManyRelationshipsDocSchemaName(resourceConfig.resourceType()));
        return toManyRelationshipsDocSchema;
    }

    private PrimaryAndNestedSchemas generateToOneRelationshipDocSchema(
            Resource<?> resourceConfig,
            String resourceIdentifierSchemaName
    ) {
        PrimaryAndNestedSchemas toOneRelationshipSchema = generateAllSchemasFromType(ToOneRelationshipDoc.class);
        toOneRelationshipSchema.getPrimarySchema().getProperties().put("data", new Schema<>().$ref(resourceIdentifierSchemaName));
        generateIncludedSchema(
                resourceConfig.resourceType(),
                true
        ).ifPresent(s -> toOneRelationshipSchema.getPrimarySchema().getProperties().put("included", s));
        toOneRelationshipSchema.getPrimarySchema().setName(toOneRelationshipDocSchemaName(resourceConfig.resourceType()));
        return toOneRelationshipSchema;
    }

    private Optional<Schema> generateIncludedSchema(
            ResourceType resourceType,
            boolean includingParentResourceType
    ) {
        Stream<ResourceType> toManyRelationshipResourceTypes = domainRegistry
                .getToManyRelationships(resourceType)
                .stream()
                .map(relType -> relType.getPlugin(RelationshipOasPlugin.class))
                .filter(Objects::nonNull)
                .flatMap(ext -> ext.getRelationshipTypes().stream());
        Stream<ResourceType> toOneRelationshipResourceTypes = domainRegistry
                .getToOneRelationships(resourceType)
                .stream()
                .map(relType -> relType.getPlugin(RelationshipOasPlugin.class))
                .filter(Objects::nonNull)
                .flatMap(ext -> ext.getRelationshipTypes().stream());
        List<ResourceType> resourcesForRelationships = Stream.concat(
                toManyRelationshipResourceTypes,
                toOneRelationshipResourceTypes
        ).toList();

        if (resourcesForRelationships.isEmpty()) {
            return Optional.empty();
        }

        List<ResourceType> parentResource = includingParentResourceType
                ? Collections.singletonList(resourceType)
                : Collections.<ResourceType>emptyList();
        List<ResourceType> resultingResources = Stream.concat(
                resourcesForRelationships.stream(),
                parentResource.stream()
        ).distinct().toList();

        List<Schema> schemaRefs = resultingResources
                .stream()
                .distinct()
                .map(OasSchemaNamesUtil::resourceSchemaName)
                .map(rn -> new Schema().$ref(rn))
                .toList();
        return Optional.of(new ArraySchema().items(new Schema().oneOf(schemaRefs)));
    }

}
