package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.PeerPlugin;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code fields[TYPE]} parameters describe what the sparse fieldsets plugin serves, so they are published only
 * when that plugin is there to serve them - and only where there is a body to select fields from.
 */
class SparseFieldsetsOasCustomizerTests {

    private static final String SF = "JsonApiSparseFieldsetsPlugin";

    private static OpenAPI document(String... activePlugins) {
        JsonApi4j jsonApi4j = OasIncludedTypesTestFixtures.jsonApi4j();

        PluginRegistry.PluginRegistryBuilder builder = PluginRegistry.builder();
        for (String name : activePlugins) {
            builder.register(new PeerPlugin(name));
        }
        PluginRegistry peers = builder.build();

        OpenAPI openApi = new OpenAPI();
        new JsonApiOperationsCustomizer(jsonApi4j).customise(openApi);
        new SparseFieldsetsOasCustomizer(() -> peers, jsonApi4j::getDomainRegistry).customise(openApi);
        return openApi;
    }

    private static List<String> paramNames(OpenAPI openApi, String path) {
        return openApi.getPaths().get(path).getGet().getParameters().stream()
                .map(Parameter::getName)
                .toList();
    }

    @Nested
    class FieldsParameters {

        @Test
        void customise_sparseFieldsetsActive_publishesOnePerSelectableType() {
            assertThat(paramNames(document(SF), "/jsonapi/articles/{id}"))
                    .contains("fields[articles]", "fields[authors]");
        }

        @Test
        void customise_sparseFieldsetsAbsent_publishesNone() {
            assertThat(paramNames(document(), "/jsonapi/articles/{id}"))
                    .noneMatch(name -> name.startsWith("fields["));
        }

        @Test
        void customise_fieldsParameter_isOptionalAndCommaSeparated() {
            Parameter fields = document(SF).getPaths().get("/jsonapi/articles/{id}").getGet().getParameters().stream()
                    .filter(parameter -> "fields[articles]".equals(parameter.getName()))
                    .findFirst()
                    .orElseThrow();

            assertThat(fields.getRequired()).isFalse();
            assertThat(fields.getSchema()).isInstanceOf(ArraySchema.class);
            assertThat(fields.getStyle()).isEqualTo(Parameter.StyleEnum.FORM);
            assertThat(fields.getExplode()).isFalse();
        }

        @Test
        void customise_primaryTypeFirst_followsTheIncludeItQualifies() {
            List<String> names = paramNames(document(SF), "/jsonapi/articles/{id}");

            assertThat(names.indexOf("include")).isLessThan(names.indexOf("fields[articles]"));
            assertThat(names.indexOf("fields[articles]")).isLessThan(names.indexOf("fields[authors]"));
        }

    }

}
