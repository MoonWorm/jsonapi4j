package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.responses.ApiResponse;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.DiagnosticsMode;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredResource;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.WriteOperations;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Customizers registered by the application are appended, never interleaved: they run once the built-in ones have
 * finished, so they see the document the framework produced and can tune anything in it. Only the document-wide
 * passes follow them - one folding what they left behind, one reading it - and neither changes what the document
 * states, so neither can undo their work.
 */
class OasDocumentTests {

    private static JsonApi4j jsonApi4j(List<OasCustomizer> customizers) {
        return jsonApi4j(customizers, new DefaultOasProperties());
    }

    private static JsonApi4j jsonApi4j(List<OasCustomizer> customizers,
                                       DefaultOasProperties oasProperties) {
        PluginRegistry plugins = PluginRegistry.builder()
                .register(new JsonApiOasPlugin(oasProperties, customizers))
                .build();
        return JsonApi4j.builder()
                .pluginRegistry(plugins)
                .domainRegistry(DomainRegistry.builder(plugins).resource(new SecuredResource()).build())
                .operationsRegistry(OperationsRegistry.builder(plugins).operations(new SecuredOperations()).build())
                .build();
    }

    @Nested
    class RegisteredCustomizers {

        @Test
        void customizers_noneRegistered_returnsTheBuiltInsOnly() {
            assertThat(OasDocument.customizers(jsonApi4j(List.of())))
                    .noneMatch(customizer -> customizer instanceof RecordingCustomizer);
        }

        @Test
        void customizers_registered_appendsThemAfterTheBuiltIns() {
            OasCustomizer first = new RecordingCustomizer(new ArrayList<>());
            OasCustomizer second = new RecordingCustomizer(new ArrayList<>());

            List<OasCustomizer> customizers = OasDocument.customizers(jsonApi4j(List.of(first, second)));

            assertThat(customizers).containsSubsequence(first, second);
            assertThat(customizers.indexOf(first))
                    .isGreaterThan(customizers.indexOf(customizers.stream()
                            .filter(JsonApiOperationsCustomizer.class::isInstance)
                            .findFirst()
                            .orElseThrow()));
        }

        @Test
        void customizers_always_endWithTheDocumentWidePasses() {
            List<OasCustomizer> customizers = OasDocument.customizers(
                    jsonApi4j(List.of(new RecordingCustomizer(new ArrayList<>()))));

            assertThat(customizers).element(customizers.size() - 2).isInstanceOf(SharedComponentsCustomizer.class);
            assertThat(customizers).last().isInstanceOf(DocumentSelfCheckCustomizer.class);
        }

        @Test
        void generate_customizerRegistered_seesTheGeneratedDocument() {
            List<String> seenOperationIds = new ArrayList<>();

            OpenAPI openApi = OasDocument.generate(jsonApi4j(List.of(new RecordingCustomizer(seenOperationIds))));

            assertThat(seenOperationIds).containsExactly("get-single-secured");
            assertThat(openApi.getPaths()).isNotEmpty();
        }

        @Test
        void generate_customizerRegistered_canOverrideWhatTheFrameworkGenerated() {
            OasCustomizer renamer = api -> api.getPaths().values()
                    .forEach(pathItem -> pathItem.readOperations()
                            .forEach(operation -> operation.setSummary("Replaced")));

            OpenAPI openApi = OasDocument.generate(jsonApi4j(List.of(renamer)));

            assertThat(openApi.getPaths().values())
                    .flatMap(pathItem -> pathItem.readOperations())
                    .allSatisfy(operation -> assertThat(operation.getSummary()).isEqualTo("Replaced"));
        }

    }

    /**
     * A customizer adding a response OpenAPI allows but the framework never generates - {@code default}, a range
     * wildcard - is the documented way to extend the document, and it runs before the two document-wide passes. The
     * self-check runs last, so a name those passes invent that resolves to nothing fails this.
     */
    @Nested
    class CustomizerAddedResponses {

        /**
         * Three operations, so a repeated response is actually a candidate for sharing, and
         * {@link DiagnosticsMode#FAIL_ON_REQUEST} so the self-check turns a name that resolves to nothing into a
         * failure rather than a log line.
         */
        private JsonApi4j strict() {
            DefaultOasProperties oasProperties = new DefaultOasProperties();
            oasProperties.setDiagnostics(DiagnosticsMode.FAIL_ON_REQUEST);
            PluginRegistry plugins = PluginRegistry.builder()
                    .register(new JsonApiOasPlugin(oasProperties, List.of(new ExtraResponsesCustomizer())))
                    .build();
            return JsonApi4j.builder()
                    .pluginRegistry(plugins)
                    .domainRegistry(DomainRegistry.builder(plugins).resource(new SecuredResource()).build())
                    .operationsRegistry(OperationsRegistry.builder(plugins).operations(new WriteOperations()).build())
                    .build();
        }

        @Test
        void generate_customizerAddsNonNumericResponses_producesResolvableReferences() {
            OpenAPI openApi = OasDocument.generate(strict());

            assertThat(openApi.getComponents().getResponses()).containsKeys("Default", "Status4XX");
        }

        @Test
        void generate_customizerAddsNonNumericResponses_leavesEveryOperationPointingAtThem() {
            OpenAPI openApi = OasDocument.generate(strict());

            assertThat(openApi.getPaths().values())
                    .flatMap(pathItem -> pathItem.readOperations())
                    .allSatisfy(operation -> {
                        assertThat(operation.getResponses().get("default").get$ref())
                                .isEqualTo("#/components/responses/Default");
                        assertThat(operation.getResponses().get("4XX").get$ref())
                                .isEqualTo("#/components/responses/Status4XX");
                    });
        }

    }

    /**
     * The self-check exists only to report, so the mode that reports nothing should not pay for the walk over the
     * whole document either.
     */
    @Nested
    class SelfCheck {

        private JsonApi4j withDiagnostics(DiagnosticsMode diagnostics) {
            DefaultOasProperties oasProperties = new DefaultOasProperties();
            oasProperties.setDiagnostics(diagnostics);
            return jsonApi4j(List.of(), oasProperties);
        }

        @Test
        void customizers_diagnosticsDisabled_omitsTheSelfCheckEntirely() {
            assertThat(OasDocument.customizers(withDiagnostics(DiagnosticsMode.DISABLED)))
                    .noneMatch(DocumentSelfCheckCustomizer.class::isInstance);
        }

        @ParameterizedTest
        @EnumSource(value = DiagnosticsMode.class, names = "DISABLED", mode = EnumSource.Mode.EXCLUDE)
        void customizers_diagnosticsEnabled_runsTheSelfCheckLast(DiagnosticsMode diagnostics) {
            assertThat(OasDocument.customizers(withDiagnostics(diagnostics)))
                    .last().isInstanceOf(DocumentSelfCheckCustomizer.class);
        }

        @Test
        void customizers_diagnosticsDisabled_stillFoldsSharedComponents() {
            assertThat(OasDocument.customizers(withDiagnostics(DiagnosticsMode.DISABLED)))
                    .last().isInstanceOf(SharedComponentsCustomizer.class);
        }

    }

    private static final class ExtraResponsesCustomizer implements OasCustomizer {

        @Override
        public void customise(OpenAPI openApi) {
            openApi.getPaths().values().stream()
                    .flatMap(pathItem -> pathItem.readOperations().stream())
                    .forEach(operation -> {
                        operation.getResponses().addApiResponse("default",
                                new ApiResponse().description("Unexpected error."));
                        operation.getResponses().addApiResponse("4XX",
                                new ApiResponse().description("Client error."));
                    });
        }

    }

    private record RecordingCustomizer(List<String> seenOperationIds) implements OasCustomizer {

        @Override
        public void customise(OpenAPI openApi) {
            openApi.getPaths().values().stream()
                    .flatMap(pathItem -> pathItem.readOperations().stream())
                    .forEach(operation -> seenOperationIds.add(operation.getOperationId()));
        }

    }

}
