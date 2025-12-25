package pro.api4.jsonapi4j.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Data;
import org.apache.commons.collections4.MapUtils;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasRelationshipInfo;
import pro.api4.jsonapi4j.domain.plugin.oas.model.OasResourceInfo;
import pro.api4.jsonapi4j.model.document.data.*;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;

import java.util.*;
import java.util.stream.Stream;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static pro.api4.jsonapi4j.oas.customizer.util.OasSchemaNamesUtil.*;
import static pro.api4.jsonapi4j.oas.customizer.util.SchemaGeneratorUtil.*;

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
        domainRegistry.getRegisteredResources()
                .stream()
                .sorted()
                .flatMap(resourceConfig -> generateSchemasForResource(resourceConfig).stream())
                .forEach(s -> registerSchemaIfNotExists(s, openApi));
    }

    private List<Schema> generateSchemasForResource(RegisteredResource<Resource<?>> registeredResource) {

        PrimaryAndNestedSchemas attAndNestedSchemas = generateJsonApiAttributesSchema(registeredResource);

        PrimaryAndNestedSchemas defaultToManyRelationshipsDocSchemas = generateAllSchemasFromType(ToManyRelationshipsDoc.class);
        PrimaryAndNestedSchemas defaultToOneRelationshipDocSchemas = generateAllSchemasFromType(ToOneRelationshipDoc.class);
        Optional<PrimaryAndNestedSchemas> relationshipsSchemas = generateJsonApiRelationshipsSchema(
                registeredResource,
                defaultToManyRelationshipsDocSchemas.getPrimarySchema().getName(),
                defaultToOneRelationshipDocSchemas.getPrimarySchema().getName()
        );

        PrimaryAndNestedSchemas resourceSchemas = generateResourceSchema(
                registeredResource.getResource(),
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

        ResourceType resourceType = registeredResource.getResource().resourceType();
        if (operationsRegistry.isAnyResourceOperationConfigured(resourceType)) {
            PrimaryAndNestedSchemas singleResourceDocSchemas = generateSingleResourceDocSchema(
                    registeredResource.getResource(),
                    resourceSchemas.getPrimarySchema()
            );
            PrimaryAndNestedSchemas multipleResourcesDocSchemas = generateMultipleResourcesDocSchema(
                    registeredResource.getResource(),
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
                    registeredResource.getResource(),
                    resourceIdentifierSchema.getName()
            );
            schemas.add(toManyRelationshipsDocSchemas.getPrimarySchema());
            schemas.addAll(toManyRelationshipsDocSchemas.getNestedSchemas());
        }
        if (isAnyToOneRelationshipsConfigured) {
            PrimaryAndNestedSchemas toOneRelationshipDocSchemas = generateToOneRelationshipDocSchema(
                    registeredResource.getResource(),
                    resourceIdentifierSchema.getName()
            );
            schemas.add(toOneRelationshipDocSchemas.getPrimarySchema());
            schemas.addAll(toOneRelationshipDocSchemas.getNestedSchemas());
        }
        return schemas;
    }

    private PrimaryAndNestedSchemas generateJsonApiAttributesSchema(RegisteredResource<Resource<?>> registeredResource) {
        PrimaryAndNestedSchemas result;
        Object pluginInfo = emptyIfNull(registeredResource.getPluginInfo()).get(JsonApiOasPlugin.NAME);
        if (pluginInfo != null && (pluginInfo instanceof OasResourceInfo oasResourceInfo)) {
            Class<?> attClass = oasResourceInfo.attributes();
            result = generateAllSchemasFromType(attClass);
        } else {
            result = new PrimaryAndNestedSchemas(new Schema(), Collections.emptyList());
        }
        result.getPrimarySchema().setName(attributesSchemaName(registeredResource.getResource().resourceType()));
        return result;
    }

    private Optional<PrimaryAndNestedSchemas> generateJsonApiRelationshipsSchema(
            RegisteredResource<Resource<?>> registeredResource,
            String toManyRelationshipsDocSchemaName,
            String toOneRelationshipDocSchemaName) {
        Resource<?> resource = registeredResource.getResource();
        ResourceType resourceType = resource.resourceType();

        Schema relationshipsSchema = new Schema<>();
        List<Schema> nestedSchemas = new ArrayList<>();
        PrimaryAndNestedSchemas result = new PrimaryAndNestedSchemas(relationshipsSchema, nestedSchemas);

        List<String> relationshipNames = new ArrayList<>();

        Map<String, Schema> relationshipsSchemaProperties = new HashMap<>();
        for (RegisteredRelationship<ToManyRelationship<?, ?>> registeredRelationship
                : domainRegistry.getRegisteredToManyRelationships(resourceType)) {
            Relationship<?, ?> relationship = registeredRelationship.getRelationship();
            ResourceType relResourceType = relationship.resourceType();
            RelationshipName relationshipName = relationship.relationshipName();
            relationshipNames.add(relationshipName.getName());

            Object pluginInfo = emptyIfNull(registeredRelationship.getPluginInfo()).get(JsonApiOasPlugin.NAME);
            if (pluginInfo != null
                    && (pluginInfo instanceof OasRelationshipInfo oasRelationshipInfo)
                    && oasRelationshipInfo.resourceLinkageMetaType() != OasRelationshipInfo.NoLinkageMeta.class) {
                PrimaryAndNestedSchemas customToManyRelationshipsDocSchema = generateCustomToManyRelationshipsDocSchema(
                        relResourceType,
                        relationshipName,
                        oasRelationshipInfo.resourceLinkageMetaType()
                );
                result.addToNested(customToManyRelationshipsDocSchema);
                relationshipsSchemaProperties.put(
                        relationshipName.getName(),
                        new Schema<>().$ref(customToManyRelationshipsDocSchema.getPrimarySchema().getName())
                );
            } else {
                relationshipsSchemaProperties.put(
                        relationshipName.getName(),
                        new Schema<>().$ref(toManyRelationshipsDocSchemaName)
                );
            }
        }
        for (RegisteredRelationship<ToOneRelationship<?, ?>> registeredRelationship
                : domainRegistry.getRegisteredToOneRelationships(resourceType)) {
            Relationship<?, ?> relationship = registeredRelationship.getRelationship();
            ResourceType relResourceType = relationship.resourceType();
            RelationshipName relationshipName = relationship.relationshipName();
            relationshipNames.add(relationshipName.getName());

            Object pluginInfo = emptyIfNull(registeredRelationship.getPluginInfo()).get(JsonApiOasPlugin.NAME);
            if (pluginInfo != null
                    && (pluginInfo instanceof OasRelationshipInfo oasRelationshipInfo)
                    && oasRelationshipInfo.resourceLinkageMetaType() != OasRelationshipInfo.NoLinkageMeta.class) {
                PrimaryAndNestedSchemas customToOneRelationshipDocSchema = generateCustomToOneRelationshipDocSchema(
                        relResourceType,
                        relationshipName,
                        oasRelationshipInfo.resourceLinkageMetaType()
                );
                relationshipsSchemaProperties.put(
                        relationshipName.getName(),
                        new Schema<>().$ref(customToOneRelationshipDocSchema.getPrimarySchema().getName())
                );
            } else {
                relationshipsSchemaProperties.put(
                        relationshipName.getName(),
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
        relationshipsSchema.setName(relationshipsSchemaName(resourceType));

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
        Stream<String> toManyRelationshipResourceTypes = domainRegistry
                .getRegisteredToManyRelationships(resourceType)
                .stream()
                .map(relType -> MapUtils.emptyIfNull(relType.getPluginInfo()).get(JsonApiOasPlugin.NAME))
                .filter(Objects::nonNull)
                .filter(r -> r instanceof OasRelationshipInfo)
                .map(r -> (OasRelationshipInfo) r)
                .flatMap(r -> Arrays.stream(r.relationshipTypes()));
        Stream<String> toOneRelationshipResourceTypes = domainRegistry
                .getRegisteredToOneRelationships(resourceType)
                .stream()
                .map(relType -> MapUtils.emptyIfNull(relType.getPluginInfo()).get(JsonApiOasPlugin.NAME))
                .filter(Objects::nonNull)
                .filter(r -> r instanceof OasRelationshipInfo)
                .map(r -> (OasRelationshipInfo) r)
                .flatMap(r -> Arrays.stream(r.relationshipTypes()));
        List<String> resourcesForRelationships = Stream.concat(
                toManyRelationshipResourceTypes,
                toOneRelationshipResourceTypes
        ).toList();

        if (resourcesForRelationships.isEmpty()) {
            return Optional.empty();
        }

        List<ResourceType> parentResource = includingParentResourceType
                ? Collections.singletonList(resourceType)
                : Collections.<ResourceType>emptyList();
        List<String> resultingResources = Stream.concat(
                resourcesForRelationships.stream(),
                parentResource.stream().map(ResourceType::getType)
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
