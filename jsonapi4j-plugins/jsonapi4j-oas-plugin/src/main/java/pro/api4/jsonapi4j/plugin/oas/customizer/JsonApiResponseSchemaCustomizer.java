package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import org.apache.commons.collections4.CollectionUtils;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Discriminator;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import lombok.Getter;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.*;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasIncludableTypesUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasLinkageMetaUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceInfoUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceTypes;
import pro.api4.jsonapi4j.plugin.oas.domain.model.OasResourceInfoModel;
import pro.api4.jsonapi4j.model.document.LinkObject;
import pro.api4.jsonapi4j.model.document.BaseDoc;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.request.CursorAwareRequest;
import pro.api4.jsonapi4j.model.document.data.*;
import pro.api4.jsonapi4j.model.document.error.ErrorsDoc;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.operation.OperationType;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;

import java.util.*;

import static org.apache.commons.collections4.MapUtils.emptyIfNull;
import static pro.api4.jsonapi4j.processor.resolvers.links.toplevel.MultiResourcesDocMetaDefaultResolvers.PAGINATION_NEXT_CURSOR_KEY_META;
import static pro.api4.jsonapi4j.processor.resolvers.links.toplevel.MultiResourcesDocMetaDefaultResolvers.PAGINATION_TOTAL_ITEMS_KEY_META;
import static pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject.ID_FIELD;
import static pro.api4.jsonapi4j.model.document.data.SingleResourceDoc.INCLUDED_FIELD;
import static pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject.TYPE_FIELD;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil.*;
import static pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil.*;

@SuppressWarnings({"rawtypes", "unchecked"})
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
        registerPaginationSchemas(openApi);
        registerResourceIdentifierObjectSchema(openApi);
        registerErrorDocSchemas(openApi);
        registerDataDocsSchemas(openApi);
    }

    private void registerLinksObjectSchema(OpenAPI openApi) {
        Schema<Object> linksObjectSchema = new Schema<>();
        linksObjectSchema.setName(LinksObject.class.getSimpleName());
        linksObjectSchema.setType("object");
        linksObjectSchema.setDescription("A JSON:API links object. May contain additional custom links beyond the standard ones.");
        linksObjectSchema.setAdditionalProperties(linkValueSchema());
        registerSchemaIfNotExists(linksObjectSchema, openApi);
    }

    /**
     * A JSON:API link is either a URL string or a link object, so every link member - named or not - is that union.
     */
    private Schema<Object> linkValueSchema() {
        Schema<Object> linkValueSchema = new Schema<>();
        linkValueSchema.oneOf(List.of(
                new Schema<String>().type("string").description("A URL string"),
                generateSchemaFromType(LinkObject.class).required(List.of("href"))
        ));
        return linkValueSchema;
    }

    /**
     * Names the pagination members of {@code links} and {@code meta}, so a client can page from the document rather
     * than by guessing.
     * <p>
     * Which of them a given response carries is up to what the operation can compute - {@code next} stops once there
     * are no further pages, and {@code prev} and {@code last} need a position or a total that a cursor-paged
     * operation may not have - so all of them are optional and say so. What matters is that they are discoverable at
     * all: without this the whole contract was an untyped map.
     */
    private void registerPaginationSchemas(OpenAPI openApi) {
        Schema<Object> paginationLinks = new Schema<>();
        paginationLinks.setName(OasSchemaNamesUtil.paginationLinksObjectSchemaName());
        paginationLinks.setType("object");
        paginationLinks.setDescription("Links for paging through the collection, alongside any custom links. Every "
                + "member is optional: which ones a response carries depends on what the operation can work out.");
        paginationLinks.setAdditionalProperties(linkValueSchema());
        paginationLinks.addProperty(LinksObject.SELF_FIELD,
                paginationLink("This page, exactly as it was requested."));
        paginationLinks.addProperty(LinksObject.FIRST_FIELD,
                paginationLink("The first page of the collection."));
        paginationLinks.addProperty(LinksObject.PREV_FIELD,
                paginationLink("The previous page. Absent on the first page, and whenever the operation cannot "
                        + "address a page backwards."));
        paginationLinks.addProperty(LinksObject.NEXT_FIELD,
                paginationLink(String.format(
                        "The next page, ready to follow. Absent once there are no further pages. Equivalent to "
                                + "sending '%s' back as '%s'.",
                        PAGINATION_NEXT_CURSOR_KEY_META, CursorAwareRequest.CURSOR_PARAM)));
        paginationLinks.addProperty(LinksObject.LAST_FIELD,
                paginationLink("The last page. Absent unless the operation knows how many there are."));
        registerSchemaIfNotExists(paginationLinks, openApi);

        Schema<Object> paginationMeta = new Schema<>();
        paginationMeta.setName(OasSchemaNamesUtil.paginationMetaObjectSchemaName());
        paginationMeta.setType("object");
        paginationMeta.setDescription("Pagination metadata. Open to whatever else the application puts in 'meta'.");
        paginationMeta.setAdditionalProperties(true);
        paginationMeta.addProperty(PAGINATION_NEXT_CURSOR_KEY_META, new StringSchema()
                .description(String.format(
                        "Cursor for the next page - send it back as '%s'. Absent once there are no further pages.",
                        CursorAwareRequest.CURSOR_PARAM)));
        paginationMeta.addProperty(PAGINATION_TOTAL_ITEMS_KEY_META, new IntegerSchema()
                .format("int64")
                .description("Total number of items across every page. Absent unless the operation counts them."));
        registerSchemaIfNotExists(paginationMeta, openApi);
    }

    /**
     * A URL string rather than the string-or-link-object union {@code additionalProperties} allows: the builders that
     * produce these ({@code LinksObject.builder().next(String)}) take an href, so a pagination link is never an
     * object. Saying so keeps the schema both accurate and readable - the union inlined per member buried the
     * descriptions that are the point of naming them.
     */
    private Schema<String> paginationLink(String description) {
        return new StringSchema().description(description);
    }

    /**
     * Points a paginated document's {@code links} and {@code meta} at the schemas that name their pagination members.
     * Only the documents a paginated operation answers with get these - a document nested inside a resource's
     * {@code relationships} is not a page of anything.
     */
    private PrimaryAndNestedSchemas withPaginationRefs(PrimaryAndNestedSchemas schemas) {
        Schema<?> doc = schemas.getPrimarySchema();
        if (doc.getProperties() == null) {
            return schemas;
        }
        if (doc.getProperties().containsKey(BaseDoc.LINKS_FIELD)) {
            doc.getProperties().put(BaseDoc.LINKS_FIELD,
                    new Schema<>().$ref(OasSchemaNamesUtil.paginationLinksObjectSchemaName()));
        }
        if (doc.getProperties().containsKey(BaseDoc.META_FIELD)) {
            doc.getProperties().put(BaseDoc.META_FIELD,
                    new Schema<>().$ref(OasSchemaNamesUtil.paginationMetaObjectSchemaName()));
        }
        return schemas;
    }

    private void registerResourceIdentifierObjectSchema(OpenAPI openApi) {
        Schema<?> resourceIdentifierObjectSchema = generateSchemaFromType(ResourceIdentifierObject.class);
        ((Schema) resourceIdentifierObjectSchema.getProperties().get(ID_FIELD))
                .example("12345")
                .description("Linked resource unique identifier");
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
        // registerResourceIdentifierObjectSchema already publishes this one, described; only its name is needed here
        String resourceIdentifierSchemaName = ResourceIdentifierObject.class.getSimpleName();
        boolean isAnyToManyRelationshipsConfigured
                = operationsRegistry.isAnyToManyRelationshipOperationConfigured(resourceType);
        boolean isAnyToOneRelationshipsConfigured
                = operationsRegistry.isAnyToOneRelationshipOperationConfigured(resourceType);
        if (isAnyToManyRelationshipsConfigured) {
            PrimaryAndNestedSchemas toManyRelationshipsDocSchemas = generateToManyRelationshipDocSchema(
                    registeredResource,
                    resourceIdentifierSchemaName
            );
            schemas.add(toManyRelationshipsDocSchemas.getPrimarySchema());
            schemas.addAll(toManyRelationshipsDocSchemas.getNestedSchemas());
        }
        if (isAnyToOneRelationshipsConfigured) {
            PrimaryAndNestedSchemas toOneRelationshipDocSchemas = generateToOneRelationshipDocSchema(
                    registeredResource,
                    resourceIdentifierSchemaName
            );
            schemas.add(toOneRelationshipDocSchemas.getPrimarySchema());
            schemas.addAll(toOneRelationshipDocSchemas.getNestedSchemas());
        }
        return schemas;
    }

    /**
     * The attributes a response may carry - none of them required.
     * <p>
     * A required attribute is a promise that it is always there, and a response is in no position to make it. An
     * attribute with no value is not serialized at all; sparse fieldsets let a client ask for a subset; access
     * control withholds what the caller may not see. Each of those produces a response that a {@code required} list
     * copied from the attributes class would reject, and a generated client would model the attribute as
     * non-nullable and fail to read it.
     * <p>
     * What a write must carry is a different question with a different answer, so it gets a schema of its own - see
     * {@link OasSchemaNamesUtil#requestAttributesSchemaName(ResourceType)}.
     */
    private PrimaryAndNestedSchemas generateJsonApiAttributesSchema(RegisteredResource<Resource<?>> registeredResource) {
        PrimaryAndNestedSchemas result;
        Object pluginInfo = emptyIfNull(registeredResource.getPluginInfo()).get(JsonApiOasPlugin.NAME);
        if (pluginInfo != null && (pluginInfo instanceof OasResourceInfoModel oasResourceInfo)) {
            Class<?> attClass = oasResourceInfo.getAttributes();
            result = generateAllSchemasFromType(attClass);
        } else {
            result = new PrimaryAndNestedSchemas(new Schema(), Collections.emptyList());
        }
        ResourceType resourceType = registeredResource.getResourceType();
        Schema<?> attributesSchema = result.getPrimarySchema();
        attributesSchema.setName(attributesSchemaName(resourceType));
        relaxRequired(attributesSchema, resourceType);
        return result;
    }

    private void relaxRequired(Schema<?> attributesSchema,
                               ResourceType resourceType) {
        if (CollectionUtils.isEmpty(attributesSchema.getRequired())) {
            return;
        }
        attributesSchema.setRequired(null);
        appendDescription(attributesSchema, attributesDisclaimer(resourceType));
    }

    /**
     * Points at the request form only when there is one - a read-only resource has no write schema to compare
     * against, and naming one that the document does not publish would send a reader looking for it.
     */
    /**
     * Says only what is true of every application: an attribute with no value is not serialized, so a response never
     * guarantees one. Plugins that narrow a response further - sparse fieldsets, access control - add their own
     * sentence from their own customizer, so an application running neither is not told about them.
     */
    private String attributesDisclaimer(ResourceType resourceType) {
        String disclaimer = "No attribute is guaranteed to be present in a response: one with no value is omitted.";
        return operationsRegistry.isResourceOperationConfigured(resourceType, OperationType.CREATE_RESOURCE)
                ? disclaimer + String.format(
                        " See %s for what a create must carry.",
                        OasSchemaNamesUtil.createAttributesSchemaName(resourceType))
                : disclaimer;
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

        Optional<OasResourceInfoModel> resourceInfo = OasResourceInfoUtil.resourceInfo(registeredResource);
        ((Schema) resourceSchema.getPrimarySchema().getProperties().get(ID_FIELD))
                .example(OasResourceInfoUtil.resourceIdExample(resourceInfo))
                .description(OasResourceInfoUtil.resourceIdDescription(resourceInfo));
        pinResourceType(
                (Schema) resourceSchema.getPrimarySchema().getProperties().get(TYPE_FIELD),
                registeredResource.getResourceType()
        );

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
        PrimaryAndNestedSchemas multipleResourceSchema = withPaginationRefs(
                withLinksObjectRef(generateAllSchemasFromType(MultipleResourcesDoc.class)));
        multipleResourceSchema.getPrimarySchema().getProperties().put("data", new ArraySchema().items(new Schema<>().$ref(resourceSchema.getName())));
        applyIncludedSchema(multipleResourceSchema, generateIncludedSchema(registeredResource.getResourceType(), false));
        multipleResourceSchema.getPrimarySchema().setName(multipleResourcesDocSchemaName(registeredResource.getResourceType()));
        return multipleResourceSchema;
    }

    private PrimaryAndNestedSchemas generateToManyRelationshipDocSchema(
            RegisteredResource<Resource<?>> registeredResource,
            String resourceIdentifierSchemaName
    ) {
        PrimaryAndNestedSchemas toManyRelationshipsDocSchema = withPaginationRefs(
                withLinksObjectRef(generateAllSchemasFromType(ToManyRelationshipsDoc.class)));
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
        Schema<?> itemSchema = new Schema<>()
                .oneOf(schemaRefs)
                .discriminator(resourceTypeDiscriminator(resultingResourceTypes));
        return Optional.of(new ArraySchema().items(itemSchema));
    }

    /**
     * JSON:API's {@code type} member is what tells one member of {@code included} from another, so it is published as
     * the discriminator too. Without it a generated client hands the caller an untyped union to switch on by hand and
     * a validator has to try every branch. The mapping is explicit because a resource type ({@code users}) is not the
     * name of the schema describing it ({@code UsersResource}).
     */
    /**
     * A resource's {@code type} is not a free string - it is the one value JSON:API allows for that resource, and it
     * is what the framework answers a mismatched request body with a {@code 409} over. Publishing it as a single-value
     * enum says so, and lets the {@code included} discriminator resolve a member by reading it.
     */
    private void pinResourceType(Schema typeSchema,
                                 ResourceType resourceType) {
        typeSchema.addEnumItemObject(resourceType.getType());
        typeSchema.setExample(resourceType.getType());
        typeSchema.setDescription("Resource type");
    }

    private Discriminator resourceTypeDiscriminator(Set<ResourceType> resourceTypes) {
        Discriminator discriminator = new Discriminator().propertyName(TYPE_FIELD);
        resourceTypes.forEach(resourceType -> discriminator.mapping(
                resourceType.getType(),
                SCHEMAS_REF_PREFIX + OasSchemaNamesUtil.resourceSchemaName(resourceType)
        ));
        return discriminator;
    }

}
