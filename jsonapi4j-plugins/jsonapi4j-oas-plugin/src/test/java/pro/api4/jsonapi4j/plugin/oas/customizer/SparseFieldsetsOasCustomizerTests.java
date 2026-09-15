package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;

import java.util.List;
import java.util.Objects;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The {@code fields[TYPE]} parameters describe what the sparse fieldsets plugin serves. {@link OasDocument} applies
 * this customizer only when that plugin is registered and enabled, so the cases here are "applied" and "not applied"
 * rather than a check the customizer makes itself.
 */
class SparseFieldsetsOasCustomizerTests {

    private static OpenAPI document(boolean sparseFieldsetsActive) {
        JsonApi4j jsonApi4j = OasIncludedTypesTestFixtures.jsonApi4j();

        OpenAPI openApi = new OpenAPI();
        new JsonApiResponseSchemaCustomizer(jsonApi4j).customise(openApi);
        new JsonApiOperationsCustomizer(jsonApi4j).customise(openApi);
        if (sparseFieldsetsActive) {
            new SparseFieldsetsOasCustomizer(jsonApi4j).customise(openApi);
        }
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
        void customise_applied_publishesOnePerSelectableType() {
            assertThat(paramNames(document(true), "/jsonapi/articles/{id}"))
                    .contains("fields[articles]", "fields[authors]");
        }

        @Test
        void customise_notApplied_publishesNone() {
            assertThat(paramNames(document(false), "/jsonapi/articles/{id}"))
                    .noneMatch(name -> name.startsWith("fields["));
        }

        @Test
        void customise_fieldsParameter_isOptionalAndCommaSeparated() {
            Parameter fields = document(true).getPaths().get("/jsonapi/articles/{id}").getGet().getParameters().stream()
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
            List<String> names = paramNames(document(true), "/jsonapi/articles/{id}");

            assertThat(names.indexOf("include")).isLessThan(names.indexOf("fields[articles]"));
            assertThat(names.indexOf("fields[articles]")).isLessThan(names.indexOf("fields[authors]"));
        }

    }


    /**
     * The attributes schema requires nothing whether or not this plugin is around - a response never guarantees an
     * attribute. What only this plugin can say is that a client may narrow the set on purpose, so that sentence has
     * to appear with it and stay away without it.
     */
    @Nested
    class SelectableAttributesDescription {

        /**
         * Empty rather than {@code null}: a schema with nothing to relax and no plugin to describe carries no
         * description at all, which is the case the second test is about.
         */
        private String attributesDescription(boolean sparseFieldsetsActive,
                                             String schemaName) {
            return Objects.toString(
                    document(sparseFieldsetsActive).getComponents().getSchemas().get(schemaName).getDescription(),
                    "");
        }

        @Test
        void customise_applied_saysTheAttributesCanBeNarrowed() {
            assertThat(attributesDescription(true, "ArticlesAttributes"))
                    .contains("narrow what is returned with 'fields[articles]'");
        }

        @Test
        void customise_notApplied_saysNothingAboutSparseFieldsets() {
            assertThat(attributesDescription(false, "ArticlesAttributes"))
                    .doesNotContain("fields[");
        }

    }

}
