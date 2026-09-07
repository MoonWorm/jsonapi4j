package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.Getter;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasIncludableTypesUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasLinkageMetaUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceTypes;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasResourceInfoModel;
import pro.api4.jsonapi4j.model.document.LinkObject;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.*;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;

import java.util.*;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject.ID_FIELD;
import static pro.api4.jsonapi4j.model.document.data.SingleResourceDoc.INCLUDED_FIELD;
import static pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject.TYPE_FIELD;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.*;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.*;

@SuppressWarnings("ALL")
@Getter
public class JsonApiResponseSchemaCustomizer implements OasCustomizer {

    private final DomainRegistry domainRegistry;
    private final OperationsRegistry operationsRegistry;

    public JsonApiResponseSchemaCustomizer(JsonApi4j jsonApi4j) {
        this.domainRegistry = jsonApi4j.getDomainRegistry();
        this.operationsRegistry = jsonApi4j.getOperationsRegistry();
    }

    @Override
    public void customise(OpenAPI openApi) {
        registerLinksObjectSchema(openApi);
        registerResourceIdentifierObjectSchema(openApi);
        registerErrorDocSchemas(openApi);
        registerDataDocsSchemas(openApi);
    }

    private void registerLinksObjectSchema(OpenAPI openApi) {
        Schema<Object> linkValueSchema = new Schema<>();
        linkValueSchema.oneOf(List.of(
                new Schema<String>().type("string").description("A URL string"),
                generateSchemaFromType(LinkObject.class).required(List.of("href"))
        ));

        Schema<Object> linksObjectSchema = new Schema<>();
        linksObjectSchema.setName(LinksObject.class.getSimpleName());
        linksObjectSchema.setType("object");
        linksObjectSchema.setDescription("A JSON:API links object. May contain additional custom links beyond the standard ones.");
        linksObjectSchema.setAdditionalProperties(linkValueSchema);
        registerSchemaIfNotExists(linksObjectSchema, openApi);
    }

    private void registerResourceIdentifierObjectSchema(OpenAPI openApi) {
        Schema<?> resourceIdentifierObjectSchema = generateSchemaFromType(ResourceIdentifierObject.class);
        ((Schema) resourceIdentifierObjectSchema.getProperties().get(ID_FIELD))
                .example("12345")
                .description("Linked resourc unique identifier");
        ((Schema) resourceIdentifierObjectSchema.getProperties().get(TYPE_FIELD))
                .example("articles")
                .description("Linked resource type");
        registerSchemaIfNotExists(resourceIdentifierObjectSchema, openApi);
    }

    private void registerErrorDocSchemas(OpenAPI openApi) {
        PrimaryAndNestedSchemas errorDocSchemas = withLinksObjectRef(generateAllSchemasFromType(ErrorsDoc.class));
        errorDocSchemas.getPrimarySchema().setName(errorsDocSchemaName());
        registerSchemaIfNotExists(errorDocSchemas.getPrimarySchema(), openApi);
        errorDocSchemas.getNestedSchemas().forEach(s -> registerSchemaIfNotExists(s, openApi));
    }

    private void registerDataDocsSchemas(OpenAPI openApi) {
        OasResourceTypes.registeredResourcesExcludingMeta(domainRegistry)
                .flatMap(resourceConfig -> generateSchemasForResource(resourceConfig).stream())
                .sorted(Comparator.comparing(schema -> schema.getName()))
                .forEach(s -> registerSchemaIfNotExists(s, openApi));
    }

    private List<Schema> generateSchemasForResource(RegisteredResource<Resource<?>> registeredResource) {

        PrimaryAndNestedSchemas attAndNestedSchemas = generateJsonApiAttributesSchema(registeredResource);

        // these two describe a relationship nested in a resource, where a top-level `included` has no meaning
        PrimaryAndNestedSchemas defaultToManyRelationshipsDocSchemas = withoutIncluded(
                withLinksObjectRef(generateAllSchemasFromType(ToManyRelationshipsDoc.class)));
        PrimaryAndNestedSchemas defaultToOneRelationshipDocSchemas = withoutIncluded(
                withLinksObjectRef(generateAllSchemasFromType(ToOneRelationshipDoc.class)));
        Optional<PrimaryAndNestedSchemas> relationshipsSchemas = generateJsonApiRelationshipsSchema(
                registeredResource,
                defaultToManyRelationshipsDocSchemas.getPrimarySchema().getName(),
                defaultToOneRelationshipDocSchemas.getPrimarySchema().getName()
        );

        PrimaryAndNestedSchemas resourceSchemas = generateResourceSchema(
                registeredResource,
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

        ResourceType resourceType = registeredResource.getResourceType();
        // a document is published for the operations that actually return it, not for "this resource has operations"
        if (operationsRegistry.isResourceOperationConfigured(resourceType, OperationType.READ_RESOURCE_BY_ID)
                || operationsRegistry.isResourceOperationConfigured(resourceType, OperationType.CREATE_RESOURCE)) {
            PrimaryAndNestedSchemas singleResourceDocSchemas = generateSingleResourceDocSchema(
                    registeredResource,
                    resourceSchemas.getPrimarySchema()
            );
            schemas.add(singleResourceDocSchemas.getPrimarySchema());
            schemas.addAll(singleResourceDocSchemas.getNestedSchemas());
        }
        if (operationsRegistry.isResourceOperationConfigured(resourceType, OperationType.READ_MULTIPLE_RESOURCES)) {
            PrimaryAndNestedSchemas multipleResourcesDocSchemas = generateMultipleResourcesDocSchema(
                    registeredResource,
                    resourceSchemas.getPrimarySchema()
            );
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
                    registeredResource,
                    resourceIdentifierSchema.getName()
            );
            schemas.add(toManyRelationshipsDocSchemas.getPrimarySchema());
            schemas.addAll(toManyRelationshipsDocSchemas.getNestedSchemas());
        }
        if (isAnyToOneRelationshipsConfigured) {
            PrimaryAndNestedSchemas toOneRelationshipDocSchemas = generateToOneRelationshipDocSchema(
                    registeredResource,
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
        if (pluginInfo != null && (pluginInfo instanceof OasResourceInfoModel oasResourceInfo)) {
            Class<?> attClass = oasResourceInfo.getAttributes();
            result = generateAllSchemasFromType(attClass);
        } else {
            result = new PrimaryAndNestedSchemas(new Schema(), Collections.emptyList());
        }
        result.getPrimarySchema().setName(attributesSchemaName(registeredResource.getResourceType()));
        return result;
    }

    private Optional<PrimaryAndNestedSchemas> generateJsonApiRelationshipsSchema(
            RegisteredResource<Resource<?>> registeredResource,
            String toManyRelationshipsDocSchemaName,
            String toOneRelationshipDocSchemaName) {
        ResourceType resourceType = registeredResource.getResourceType();

        Schema relationshipsSchema = new Schema<>();
        List<Schema> nestedSchemas = new ArrayList<>();
        PrimaryAndNestedSchemas result = new PrimaryAndNestedSchemas(relationshipsSchema, nestedSchemas);

        List<String> relationshipNames = new ArrayList<>();

        Map<String, Schema> relationshipsSchemaProperties = new HashMap<>();
        for (RegisteredRelationship<ToManyRelationship<?>> registeredRelationship
                : domainRegistry.getToManyRelationships(resourceType)) {
            ResourceType relResourceType = registeredRelationship.getParentResourceType();
            RelationshipName relationshipName = registeredRelationship.getRelationshipName();
            relationshipNames.add(relationshipName.getName());

            Class<?> linkageMetaType = OasLinkageMetaUtil.resolveLinkageMetaType(registeredRelationship);
            if (linkageMetaType != null) {
                PrimaryAndNestedSchemas customToManyRelationshipsDocSchema = generateCustomToManyRelationshipsDocSchema(
                        relResourceType,
                        relationshipName,
                        linkageMetaType
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
        for (RegisteredRelationship<ToOneRelationship<?>> registeredRelationship
                : domainRegistry.getToOneRelationships(resourceType)) {
            ResourceType relResourceType = registeredRelationship.getParentResourceType();
            RelationshipName relationshipName = registeredRelationship.getRelationshipName();
            relationshipNames.add(relationshipName.getName());

            Class<?> linkageMetaType = OasLinkageMetaUtil.resolveLinkageMetaType(registeredRelationship);
            if (linkageMetaType != null) {
                PrimaryAndNestedSchemas customToOneRelationshipDocSchema = generateCustomToOneRelationshipDocSchema(
                        relResourceType,
                        relationshipName,
                        linkageMetaType
                );
                result.addToNested(customToOneRelationshipDocSchema);
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
        relationshipsSchema.setProperties(relationshipsSchemaProperties);
        relationshipsSchema.setName(relationshipsSchemaName(resourceType));

        return Optional.of(result);
    }

    private PrimaryAndNestedSchemas generateCustomToManyRelationshipsDocSchema(
            ResourceType resourceType,
            RelationshipName relationshipName,
            Class<?> dataItemMetaClass
    ) {
        Schema<?> customToManyRelationshipsDocSchema = withLinksObjectRef(generateSchemaFromType(ToManyRelationshipsDoc.class));
        customToManyRelationshipsDocSchema.getProperties().remove(INCLUDED_FIELD);
        customToManyRelationshipsDocSchema.setName(customToManyRelationshipDocSchemaName(resourceType, relationshipName));

        PrimaryAndNestedSchemas identifierSchemas = OasLinkageMetaUtil.customResourceIdentifierSchemas(
                resourceType,
                relationshipName,
                dataItemMetaClass
        );

        ArraySchema dataSchema = (ArraySchema) customToManyRelationshipsDocSchema.getProperties().get("data");
        dataSchema.setItems(new Schema<>().$ref(identifierSchemas.getPrimarySchema().getName()));

        PrimaryAndNestedSchemas result = new PrimaryAndNestedSchemas(customToManyRelationshipsDocSchema, List.of());
        result.addToNested(identifierSchemas);
        return result;
    }

    private PrimaryAndNestedSchemas generateCustomToOneRelationshipDocSchema(
            ResourceType resourceType,
            RelationshipName relationshipName,
            Class<?> dataItemMetaClass
    ) {
        Schema<?> customToOneRelationshipDocSchema = withLinksObjectRef(generateSchemaFromType(ToOneRelationshipDoc.class));
        customToOneRelationshipDocSchema.getProperties().remove(INCLUDED_FIELD);
        customToOneRelationshipDocSchema.setName(customToOneRelationshipDocSchemaName(resourceType, relationshipName));

        PrimaryAndNestedSchemas identifierSchemas = OasLinkageMetaUtil.customResourceIdentifierSchemas(
                resourceType,
                relationshipName,
                dataItemMetaClass
        );

        customToOneRelationshipDocSchema.getProperties().put("data", new Schema<>().$ref(identifierSchemas.getPrimarySchema().getName()));

        PrimaryAndNestedSchemas result = new PrimaryAndNestedSchemas(customToOneRelationshipDocSchema, List.of());
        result.addToNested(identifierSchemas);
        return result;
    }

    private PrimaryAndNestedSchemas generateResourceSchema(RegisteredResource<Resource<?>> registeredResource,
                                                           Schema<?> attributesSchema,
                                                           Optional<Schema<?>> relationshipsSchema) {
        PrimaryAndNestedSchemas resourceSchema = withLinksObjectRef(generateAllSchemasFromType(ResourceObject.class));

        ((Schema) resourceSchema.getPrimarySchema().getProperties().get(ID_FIELD))
                .example("12345")
                .description("Resource unique identifier");
        ((Schema) resourceSchema.getPrimarySchema().getProperties().get(TYPE_FIELD))
                .example(registeredResource.getResourceType().getType())
                .description("Resource type");

        resourceSchema.getPrimarySchema().setRequired(List.of(ID_FIELD, TYPE_FIELD));

        resourceSchema.getPrimarySchema().setName(resourceSchemaName(registeredResource.getResourceType()));
        resourceSchema.getPrimarySchema().getProperties().put("attributes", new Schema<>().$ref(attributesSchema.getName()));
        relationshipsSchema.ifPresentOrElse(rs -> {
            resourceSchema.getPrimarySchema().getProperties().put("relationships", new Schema<>().$ref(rs.getName()));
        }, () -> resourceSchema.getPrimarySchema().getProperties().remove("relationships"));
        return resourceSchema;
    }

    private PrimaryAndNestedSchemas generateSingleResourceDocSchema(
            RegisteredResource<Resource<?>> registeredResource,
            Schema<?> resourceSchema
    ) {
        PrimaryAndNestedSchemas singleResourceDocSchema = withLinksObjectRef(generateAllSchemasFromType(SingleResourceDoc.class));
        singleResourceDocSchema.getPrimarySchema().getProperties().put("data", new Schema<>().$ref(resourceSchema.getName()));
        applyIncludedSchema(singleResourceDocSchema, generateIncludedSchema(registeredResource.getResourceType(), false));
        singleResourceDocSchema.getPrimarySchema().setName(singleResourceDocSchemaName(registeredResource.getResourceType()));
        return singleResourceDocSchema;
    }

    private PrimaryAndNestedSchemas generateMultipleResourcesDocSchema(
            RegisteredResource<Resource<?>> registeredResource,
            Schema<?> resourceSchema
    ) {
        PrimaryAndNestedSchemas multipleResourceSchema = withLinksObjectRef(generateAllSchemasFromType(MultipleResourcesDoc.class));
        multipleResourceSchema.getPrimarySchema().getProperties().put("data", new ArraySchema().items(new Schema<>().$ref(resourceSchema.getName())));
        applyIncludedSchema(multipleResourceSchema, generateIncludedSchema(registeredResource.getResourceType(), false));
        multipleResourceSchema.getPrimarySchema().setName(multipleResourcesDocSchemaName(registeredResource.getResourceType()));
        return multipleResourceSchema;
    }

    private PrimaryAndNestedSchemas generateToManyRelationshipDocSchema(
            RegisteredResource<Resource<?>> registeredResource,
            String resourceIdentifierSchemaName
    ) {
        PrimaryAndNestedSchemas toManyRelationshipsDocSchema = withLinksObjectRef(generateAllSchemasFromType(ToManyRelationshipsDoc.class));
        toManyRelationshipsDocSchema.getPrimarySchema().getProperties().put("data", new ArraySchema().items(new Schema<>().$ref(resourceIdentifierSchemaName)));
        applyIncludedSchema(toManyRelationshipsDocSchema, generateIncludedSchema(registeredResource.getResourceType(), true));
        toManyRelationshipsDocSchema.getPrimarySchema().setName(toManyRelationshipsDocSchemaName(registeredResource.getResourceType()));
        return toManyRelationshipsDocSchema;
    }

    private PrimaryAndNestedSchemas generateToOneRelationshipDocSchema(
            RegisteredResource<Resource<?>> registeredResource,
            String resourceIdentifierSchemaName
    ) {
        PrimaryAndNestedSchemas toOneRelationshipSchema = withLinksObjectRef(generateAllSchemasFromType(ToOneRelationshipDoc.class));
        toOneRelationshipSchema.getPrimarySchema().getProperties().put("data", new Schema<>().$ref(resourceIdentifierSchemaName));
        applyIncludedSchema(
                toOneRelationshipSchema,
                generateIncludedSchema(registeredResource.getResourceType(), true)
        );
        toOneRelationshipSchema.getPrimarySchema().setName(toOneRelationshipDocSchemaName(registeredResource.getResourceType()));
        return toOneRelationshipSchema;
    }

    /**
     * Publishes {@code included} only when the includable resource types are known. Otherwise the member is dropped:
     * what reflection produces for it is an untyped resource object whose generated name — {@code ResourceObject} with
     * its erased type arguments appended — leaks into the contract while saying nothing a client can use.
     */
    private PrimaryAndNestedSchemas withoutIncluded(PrimaryAndNestedSchemas schemas) {
        schemas.getPrimarySchema().getProperties().remove(INCLUDED_FIELD);
        return schemas;
    }

    private void applyIncludedSchema(PrimaryAndNestedSchemas docSchemas,
                                     Optional<Schema> includedSchema) {
        Schema<?> docSchema = docSchemas.getPrimarySchema();

        includedSchema.ifPresentOrElse(
                s -> docSchema.getProperties().put(INCLUDED_FIELD, s),
                () -> docSchema.getProperties().remove(INCLUDED_FIELD)
        );
    }

    private Optional<Schema> generateIncludedSchema(
            ResourceType parentResourceType,
            boolean includingParentResourceType
    ) {
        Set<ResourceType> includableTypes = OasIncludableTypesUtil.includableResourceTypes(
                domainRegistry,
                parentResourceType
        );
        if (includableTypes.isEmpty()) {
            return Optional.empty();
        }

        Set<ResourceType> resultingResourceTypes = new LinkedHashSet<>(includableTypes);
        if (includingParentResourceType) {
            resultingResourceTypes.add(parentResourceType);
        }

        List<Schema> schemaRefs = resultingResourceTypes
                .stream()
                .map(OasSchemaNamesUtil::resourceSchemaName)
                .map(rn -> new Schema().$ref(rn))
                .toList();
        return Optional.of(new ArraySchema().items(new Schema().oneOf(schemaRefs)));
    }

}
