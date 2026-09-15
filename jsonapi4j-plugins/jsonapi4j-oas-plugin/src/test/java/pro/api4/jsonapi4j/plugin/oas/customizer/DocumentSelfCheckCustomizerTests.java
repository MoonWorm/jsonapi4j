package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * A reference resolving to nothing is the one defect no reader forgives - client generation fails, response
 * validation rejects everything. It is also the one this plugin is most able to produce, since a schema is
 * registered by one customizer and referenced by another.
 */
class DocumentSelfCheckCustomizerTests {

    private final DocumentSelfCheckCustomizer sut = new DocumentSelfCheckCustomizer(false);
    private final DocumentSelfCheckCustomizer strictSut = new DocumentSelfCheckCustomizer(true);

    private static OpenAPI documentReferencing(String ref,
                                               String... declaredSchemas) {
        Components components = new Components();
        for (String declared : declaredSchemas) {
            components.addSchemas(declared, new Schema<>().type("object"));
        }
        Operation operation = new Operation().responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse()
                        .description("OK")
                        .content(new Content().addMediaType(
                                "application/vnd.api+json",
                                new MediaType().schema(new Schema<>().$ref(ref))))));
        return new OpenAPI()
                .components(components)
                .paths(new Paths().addPathItem("/things", new PathItem().get(operation)));
    }

    @Nested
    class DanglingReferences {

        @Test
        void customise_referenceToADeclaredComponent_isAccepted() {
            OpenAPI openApi = documentReferencing("#/components/schemas/Thing", "Thing");

            assertThatCode(() -> sut.customise(openApi)).doesNotThrowAnyException();
        }

        @Test
        void customise_referenceToNothing_failsWhenConfiguredTo() {
            OpenAPI openApi = documentReferencing("#/components/schemas/Missing", "Thing");

            assertThatThrownBy(() -> strictSut.customise(openApi))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("#/components/schemas/Missing");
        }

        @Test
        void customise_referenceToNothing_onlyWarnsByDefault() {
            OpenAPI openApi = documentReferencing("#/components/schemas/Missing", "Thing");

            assertThatCode(() -> sut.customise(openApi)).doesNotThrowAnyException();
        }

        @Test
        void customise_emptyDocument_isAccepted() {
            assertThatCode(() -> sut.customise(new OpenAPI())).doesNotThrowAnyException();
        }

    }

    /**
     * The document the plugin actually generates is the case that matters: it is assembled across five customizers,
     * and nothing but this check stands between a missed registration and a broken client.
     */
    @Nested
    class GeneratedDocument {

        /**
         * The walk must reach every reference, so a document it could not fully read is reported too - the check
         * silently covering less than it claims is the one way it could pass while the document is broken.
         */
        @Test
        void customise_generatedDocument_isFullyReadable() {
            OpenAPI openApi = OasDocument.generate(OasIncludedTypesTestFixtures.jsonApi4j());

            assertThatCode(() -> strictSut.customise(openApi)).doesNotThrowAnyException();
        }

        @Test
        void customise_generatedDocument_leavesNoReferenceUnresolved() {
            OpenAPI openApi = OasDocument.generate(OasIncludedTypesTestFixtures.jsonApi4j());

            assertThatCode(() -> strictSut.customise(openApi)).doesNotThrowAnyException();
        }

    }

}
