package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;

import java.util.List;
import java.util.stream.Stream;

/**
 * Generates the OpenAPI document for a running {@link JsonApi4j} instance.
 * <p>
 * Everything the customizers need - the registries, the root configuration and the OAS plugin's own configuration -
 * hangs off that single instance, so this is the whole of the wiring.
 */
public final class OasDocument {

    private OasDocument() {
    }

    /**
     * @param jsonApi4j the assembled framework instance
     * @return a freshly generated document describing everything {@code jsonApi4j} serves
     */
    public static OpenAPI generate(JsonApi4j jsonApi4j) {
        OpenAPI openApi = new OpenAPI();
        customizers(jsonApi4j).forEach(customizer -> customizer.customise(openApi));
        return openApi;
    }

    /**
     * The customizers in the order they must be applied: the document-wide info first, then the schemas the
     * operations refer to, then the operations, then the error examples they point at. Any customizer registered
     * with {@link JsonApiOasPlugin} follows, so it sees a finished document and can tune what the framework
     * generated - add a response, attach headers, rewrite a description.
     *
     * @param jsonApi4j the assembled framework instance
     * @return the ordered customizers, for a host that applies them itself
     */
    public static List<OasCustomizer> customizers(JsonApi4j jsonApi4j) {
        return Stream.concat(
                Stream.of(
                        new CommonOpenApiCustomizer(jsonApi4j),
                        new JsonApiResponseSchemaCustomizer(jsonApi4j),
                        new JsonApiRequestBodySchemaCustomizer(jsonApi4j),
                        new JsonApiOperationsCustomizer(jsonApi4j),
                        new ErrorExamplesCustomizer()
                ),
                jsonApi4j.getPluginRegistry()
                        .pluginOf(JsonApiOasPlugin.class)
                        .map(JsonApiOasPlugin::getCustomizers)
                        .orElseGet(List::of)
                        .stream()
        ).toList();
    }

}
