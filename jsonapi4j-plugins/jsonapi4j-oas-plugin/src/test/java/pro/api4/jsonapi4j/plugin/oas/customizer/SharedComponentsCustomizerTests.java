package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Folding repeated objects into {@code components} must not change what the document states - a {@code $ref} and the
 * object it points at are the same thing to a reader, but only while the reference resolves and only while every
 * occurrence it replaced really was identical.
 */
class SharedComponentsCustomizerTests {

    private final SharedComponentsCustomizer sut = new SharedComponentsCustomizer();

    private static ApiResponse tooManyRequests() {
        return new ApiResponse().description("Too many requests.");
    }

    private static Parameter cursor() {
        return new Parameter().name("page[cursor]").in("query").schema(new StringSchema());
    }

    private static Operation operationAnswering(String status,
                                                ApiResponse response) {
        return new Operation().responses(new ApiResponses().addApiResponse(status, response));
    }

    private static Operation operation(ApiResponse response,
                                       List<Parameter> parameters) {
        return new Operation()
                .responses(new ApiResponses().addApiResponse("429", response))
                .parameters(parameters);
    }

    private static OpenAPI document(Operation... operations) {
        Paths paths = new Paths();
        for (int i = 0; i < operations.length; i++) {
            paths.addPathItem("/p" + i, new PathItem().get(operations[i]));
        }
        return new OpenAPI().paths(paths);
    }

    @Nested
    class SharedResponses {

        @Test
        void customise_responseRepeatedIdentically_isReplacedByAReference() {
            OpenAPI openApi = document(operation(tooManyRequests(), List.of()), operation(tooManyRequests(), List.of()));

            sut.customise(openApi);

            assertThat(openApi.getComponents().getResponses()).containsKey("TooManyRequests");
            assertThat(openApi.getPaths().values())
                    .flatMap(PathItem::readOperations)
                    .allSatisfy(operation -> assertThat(operation.getResponses().get("429").get$ref())
                            .isEqualTo("#/components/responses/TooManyRequests"));
        }

        @Test
        void customise_responseAppearingOnce_isLeftInline() {
            OpenAPI openApi = document(operation(tooManyRequests(), List.of()));

            sut.customise(openApi);

            assertThat(openApi.getComponents()).isNull();
            assertThat(openApi.getPaths().get("/p0").getGet().getResponses().get("429").get$ref()).isNull();
        }

        @Test
        void customise_sameStatusWithDifferentShapes_isLeftInlineEverywhere() {
            OpenAPI openApi = document(
                    operation(tooManyRequests(), List.of()),
                    operation(new ApiResponse().description("Slow down."), List.of())
            );

            sut.customise(openApi);

            assertThat(openApi.getComponents()).isNull();
            assertThat(openApi.getPaths().values())
                    .flatMap(PathItem::readOperations)
                    .allSatisfy(operation -> assertThat(operation.getResponses().get("429").get$ref()).isNull());
        }

    }

    @Nested
    class SharedParameters {

        @Test
        void customise_parameterRepeatedIdentically_isReplacedByAReference() {
            OpenAPI openApi = document(
                    operation(tooManyRequests(), List.of(cursor())),
                    operation(tooManyRequests(), List.of(cursor()))
            );

            sut.customise(openApi);

            assertThat(openApi.getComponents().getParameters()).containsKey("PageCursor");
            assertThat(openApi.getPaths().values())
                    .flatMap(PathItem::readOperations)
                    .allSatisfy(operation -> assertThat(operation.getParameters().get(0).get$ref())
                            .isEqualTo("#/components/parameters/PageCursor"));
        }

        @Test
        void customise_bracketedParameterName_becomesALegalComponentKey() {
            Parameter fields = new Parameter().name("fields[users]").in("query").schema(new StringSchema());
            OpenAPI openApi = document(
                    operation(tooManyRequests(), List.of(fields)),
                    operation(tooManyRequests(), List.of(fields))
            );

            sut.customise(openApi);

            assertThat(openApi.getComponents().getParameters().keySet())
                    .allSatisfy(key -> assertThat(key).matches("[a-zA-Z0-9._-]+"))
                    .contains("FieldsUsers");
        }

        @Test
        void customise_parameterWithDifferentShapes_isLeftInlineEverywhere() {
            Parameter described = new Parameter().name("page[cursor]").in("query")
                    .description("Cursor").schema(new StringSchema());
            OpenAPI openApi = document(
                    operation(tooManyRequests(), List.of(cursor())),
                    operation(tooManyRequests(), List.of(described))
            );

            sut.customise(openApi);

            assertThat(openApi.getComponents().getParameters()).isNull();
        }

    }

    /**
     * A response key is not always a number: OpenAPI also allows {@code default} and the {@code 4XX} range
     * wildcards, and the documented way to add one is an application's own {@link OasCustomizer} - which runs before
     * this pass. Naming such a key must not be the thing that brings document generation down.
     */
    @Nested
    class NonNumericStatusKeys {

        @Test
        void customise_defaultResponseRepeated_isSharedRatherThanCrashing() {
            OpenAPI openApi = document(
                    operationAnswering("default", new ApiResponse().description("Unexpected error.")),
                    operationAnswering("default", new ApiResponse().description("Unexpected error.")));

            sut.customise(openApi);

            assertThat(openApi.getComponents().getResponses()).containsOnlyKeys("Default");
            assertThat(openApi.getPaths().values())
                    .flatMap(PathItem::readOperations)
                    .allSatisfy(operation -> assertThat(operation.getResponses().get("default").get$ref())
                            .isEqualTo("#/components/responses/Default"));
        }

        @Test
        void customise_rangeWildcardRepeated_isSharedUnderALegalComponentKey() {
            OpenAPI openApi = document(
                    operationAnswering("4XX", new ApiResponse().description("Client error.")),
                    operationAnswering("4XX", new ApiResponse().description("Client error.")));

            sut.customise(openApi);

            assertThat(openApi.getComponents().getResponses()).containsOnlyKeys("Status4XX");
        }

        @Test
        void customise_unknownNumericStatusRepeated_isNamedAfterTheCode() {
            OpenAPI openApi = document(
                    operationAnswering("599", new ApiResponse().description("Bespoke.")),
                    operationAnswering("599", new ApiResponse().description("Bespoke.")));

            sut.customise(openApi);

            assertThat(openApi.getComponents().getResponses()).containsOnlyKeys("Status599");
        }

    }

    @Nested
    class EmptyDocuments {

        @Test
        void customise_documentWithoutPaths_isLeftAlone() {
            OpenAPI openApi = new OpenAPI();

            sut.customise(openApi);

            assertThat(openApi.getPaths()).isNull();
            assertThat(openApi.getComponents()).isNull();
        }

    }

}
