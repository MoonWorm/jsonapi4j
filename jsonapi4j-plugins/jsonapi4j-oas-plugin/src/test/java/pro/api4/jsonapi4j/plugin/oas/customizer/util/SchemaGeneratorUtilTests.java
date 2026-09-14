package pro.api4.jsonapi4j.plugin.oas.customizer.util;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.model.document.LinksObject;
import pro.api4.jsonapi4j.model.document.data.ResourceIdentifierObject;
import pro.api4.jsonapi4j.model.document.data.SingleResourceDoc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * Schema names come from Java simple names, so nothing stops two resources from carrying a nested type of the same
 * name. First-write-wins published one of them under the other's shape and said nothing about it.
 */
class SchemaGeneratorUtilTests {

    private static Schema<?> named(String name, String propertyName) {
        Schema<?> schema = new Schema<>().type("object");
        schema.setName(name);
        schema.addProperty(propertyName, new StringSchema());
        return schema;
    }

    @Nested
    class SchemaRegistration {

        @Test
        void registerSchemaIfNotExists_sameNameDifferentShape_failsLoudly() {
            OpenAPI openApi = new OpenAPI();
            SchemaGeneratorUtil.registerSchemaIfNotExists(named("Address", "city"), openApi);

            assertThatThrownBy(() -> SchemaGeneratorUtil.registerSchemaIfNotExists(named("Address", "zip"), openApi))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Address");
        }

        @Test
        void registerSchemaIfNotExists_sameNameSameShape_isAccepted() {
            OpenAPI openApi = new OpenAPI();
            SchemaGeneratorUtil.registerSchemaIfNotExists(named("Address", "city"), openApi);

            assertThatCode(() -> SchemaGeneratorUtil.registerSchemaIfNotExists(named("Address", "city"), openApi))
                    .doesNotThrowAnyException();
            assertThat(openApi.getComponents().getSchemas()).containsOnlyKeys("Address");
        }

    }

    /**
     * {@code LinksObject} and {@code ResourceIdentifierObject} are written by hand and published once, described.
     * Reflection reaches them through any document class and produces undescribed copies - the {@code LinksObject}
     * one also carrying an {@code empty} boolean off {@code isEmpty()} that no response ever sends.
     */
    @Nested
    class HandWrittenSchemas {

        @Test
        void generateAllSchemasFromType_documentClass_contributesNoCopyOfAHandWrittenSchema() {
            assertThat(SchemaGeneratorUtil.generateAllSchemasFromType(SingleResourceDoc.class).getNestedSchemas())
                    .extracting(Schema::getName)
                    .doesNotContain(
                            LinksObject.class.getSimpleName(),
                            ResourceIdentifierObject.class.getSimpleName()
                    );
        }

    }

}
