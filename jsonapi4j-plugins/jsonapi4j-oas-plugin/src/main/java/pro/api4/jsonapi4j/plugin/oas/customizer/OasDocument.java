package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
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

    private static Stream<OasCustomizer> peerPluginCustomizers(JsonApi4j jsonApi4j) {
        List<OasCustomizer> customizers = new ArrayList<>();
        if (AccessControlOasCustomizer.isEnabledFor(jsonApi4j.getPluginRegistry())) {
            customizers.add(new AccessControlOasCustomizer(jsonApi4j));
        }
        if (SparseFieldsetsOasCustomizer.isEnabledFor(jsonApi4j.getPluginRegistry())) {
            customizers.add(new SparseFieldsetsOasCustomizer(jsonApi4j));
        }
        return customizers.stream();
    }

    private static boolean failOnMisconfiguration(JsonApi4j jsonApi4j) {
        return jsonApi4j.getPluginRegistry()
                .configOf(OasProperties.class)
                .map(OasProperties::failOnMisconfiguration)
                .orElse(false);
    }

    public static List<OasCustomizer> customizers(JsonApi4j jsonApi4j) {
        return Stream.<Stream<OasCustomizer>>of(
                        Stream.of(
                                new CommonOpenApiCustomizer(jsonApi4j),
                                new JsonApiResponseSchemaCustomizer(jsonApi4j),
                                new JsonApiRequestBodySchemaCustomizer(jsonApi4j),
                                new JsonApiOperationsCustomizer(jsonApi4j),
                                new ErrorExamplesCustomizer()
                        ),
                        peerPluginCustomizers(jsonApi4j),
                        jsonApi4j.getPluginRegistry()
                                .pluginOf(JsonApiOasPlugin.class)
                                .map(JsonApiOasPlugin::getCustomizers)
                                .orElseGet(List::<OasCustomizer>of)
                                .stream(),
                        Stream.of(new SharedComponentsCustomizer(), new DocumentSelfCheckCustomizer(failOnMisconfiguration(jsonApi4j)))
                )
                .flatMap(Function.identity())
                .toList();
    }

}
