package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.domain.ResourceType;
import pro.api4.jsonapi4j.domain.RegisteredResource;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasIncludableTypesUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasResourceTypes;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.SchemaGeneratorUtil;
import pro.api4.jsonapi4j.plugin.oas.customizer.util.OasSchemaNamesUtil;
import pro.api4.jsonapi4j.request.IncludeAwareRequest;
import pro.api4.jsonapi4j.request.SparseFieldsetsAwareRequest;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Documents the {@code fields[TYPE]} parameters this plugin makes available.
 *
 * <p>The OpenAPI plugin describes what the framework core serves and knows nothing about sparse fieldsets - a plugin
 * that adds a parameter describes that parameter itself. Register this alongside the sparse fieldsets plugin and the
 * generated document gains one {@code fields[...]} per resource type an operation's response may carry.
 *
 * <p>Operations answering with no body get none: there are no fields to select from a {@code 204}.
 */
public class SparseFieldsetsOasCustomizer implements OasCustomizer {

    /**
     * Matched by name: this module describes what the sparse fieldsets plugin contributes without depending on it,
     * and a plugin that is absent or disabled contributes nothing to describe - so this customizer is only applied
     * when {@link #isEnabledFor(PluginRegistry)} says so.
     */
    private static final String SPARSE_FIELDSETS_PLUGIN_NAME = "JsonApiSparseFieldsetsPlugin";

    private final DomainRegistry domainRegistry;

    public SparseFieldsetsOasCustomizer(JsonApi4j jsonApi4j) {
        this.domainRegistry = jsonApi4j.getDomainRegistry();
    }

    public static boolean isEnabledFor(PluginRegistry pluginRegistry) {
        return pluginRegistry != null && pluginRegistry.isActivePlugin(SPARSE_FIELDSETS_PLUGIN_NAME);
    }

    @Override
    public void customise(OpenAPI openApi) {
        describeSelectableAttributes(openApi);
        if (openApi.getPaths() == null) {
            return;
        }
        openApi.getPaths().forEach((path, pathItem) -> {
            ResourceType resourceType = resourceTypeOf(path);
            if (resourceType == null) {
                return;
            }
            pathItem.readOperations().stream()
                    .filter(this::answersWithABody)
                    .forEach(operation -> addFieldsParams(operation, resourceType));
        });
    }

    /**
     * Notes on each resource's attributes schema that a client can narrow what comes back.
     * <p>
     * The schema already requires nothing - a response never guarantees an attribute - but this plugin is one of the
     * reasons why, and only this plugin knows it is enabled. The framework's own pass says only what holds with no
     * plugins at all, so the sentence belongs here rather than there.
     */
    private void describeSelectableAttributes(OpenAPI openApi) {
        if (openApi.getComponents() == null || openApi.getComponents().getSchemas() == null) {
            return;
        }
        OasResourceTypes.registeredResourcesExcludingMeta(domainRegistry)
                .map(RegisteredResource::getResourceType)
                .forEach(resourceType -> {
                    Schema<?> attributesSchema = openApi.getComponents().getSchemas()
                            .get(OasSchemaNamesUtil.attributesSchemaName(resourceType));
                    if (attributesSchema == null) {
                        return;
                    }
                    SchemaGeneratorUtil.appendDescription(attributesSchema, String.format(
                            "A client can narrow what is returned with '%s'.",
                            SparseFieldsetsAwareRequest.getFieldsParam(resourceType.getType())));
                });
    }

    /**
     * An operation's primary resource type is the first path segment naming a registered resource - matching against
     * the registry rather than counting segments keeps this independent of where the API is mounted.
     */
    private ResourceType resourceTypeOf(String path) {
        Set<ResourceType> registered = domainRegistry.getResourceTypes();
        for (String segment : path.split("/")) {
            ResourceType candidate = new ResourceType(segment);
            if (registered.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private boolean answersWithABody(Operation operation) {
        return operation.getResponses() != null && operation.getResponses().entrySet().stream()
                .filter(response -> response.getKey().startsWith("2"))
                .map(Map.Entry::getValue)
                .anyMatch(response -> response.getContent() != null);
    }

    /**
     * Inserted straight after {@code include}, which is the parameter they qualify - a reader meets "what may come
     * back" before "which of its fields to ask for".
     */
    private void addFieldsParams(Operation operation,
                                 ResourceType resourceType) {
        Set<ResourceType> selectable = OasIncludableTypesUtil.sparseFieldsetsResourceTypes(domainRegistry, resourceType);
        if (selectable.isEmpty()) {
            return;
        }
        List<Parameter> parameters = operation.getParameters() == null
                ? new ArrayList<>()
                : new ArrayList<>(operation.getParameters());
        int at = indexAfterInclude(parameters);
        for (ResourceType selectableType : selectable) {
            parameters.add(at++, fieldsParam(selectableType));
        }
        operation.setParameters(parameters);
    }

    private int indexAfterInclude(List<Parameter> parameters) {
        for (int i = 0; i < parameters.size(); i++) {
            if (IncludeAwareRequest.INCLUDE_PARAM.equals(parameters.get(i).getName())) {
                return i + 1;
            }
        }
        return 0;
    }

    private Parameter fieldsParam(ResourceType resourceType) {
        Parameter fieldsParam = new Parameter();
        fieldsParam.setName(SparseFieldsetsAwareRequest.getFieldsParam(resourceType.getType()));
        fieldsParam.setIn("query");
        fieldsParam.setRequired(false);
        fieldsParam.setDescription(String.format(
                "Limits the attributes returned for '%s' resources to the listed ones. Nested paths are allowed "
                        + "(address.city). See the %s schema for what can be asked for. An empty value returns no attributes.",
                resourceType.getType(),
                OasSchemaNamesUtil.attributesSchemaName(resourceType)
        ));
        fieldsParam.setSchema(new ArraySchema().items(new StringSchema()));
        fieldsParam.setStyle(Parameter.StyleEnum.FORM);
        fieldsParam.setExplode(false);
        return fieldsParam;
    }

}
