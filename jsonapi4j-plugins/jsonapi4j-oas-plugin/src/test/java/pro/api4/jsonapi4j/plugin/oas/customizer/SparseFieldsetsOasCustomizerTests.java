package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;

import java.util.List;

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

}
