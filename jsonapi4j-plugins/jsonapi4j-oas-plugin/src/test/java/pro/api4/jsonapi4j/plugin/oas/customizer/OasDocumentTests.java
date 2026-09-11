package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.OperationsRegistry;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.JsonApiOasPlugin;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredResource;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Customizers registered by the application are appended, never interleaved: they run once the built-in ones have
 * finished, so they see the document the framework produced and can tune anything in it.
 */
class OasDocumentTests {

    private static JsonApi4j jsonApi4j(List<OasCustomizer> customizers) {
        PluginRegistry plugins = PluginRegistry.builder()
                .register(new JsonApiOasPlugin(new DefaultOasProperties(), customizers))
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
                    .hasSize(5)
                    .noneMatch(customizer -> customizer instanceof RecordingCustomizer);
        }

        @Test
        void customizers_registered_appendsThemAfterTheBuiltIns() {
            OasCustomizer first = new RecordingCustomizer(new ArrayList<>());
            OasCustomizer second = new RecordingCustomizer(new ArrayList<>());

            assertThat(OasDocument.customizers(jsonApi4j(List.of(first, second))))
                    .hasSize(7)
                    .endsWith(first, second);
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

    private record RecordingCustomizer(List<String> seenOperationIds) implements OasCustomizer {

        @Override
        public void customise(OpenAPI openApi) {
            openApi.getPaths().values().stream()
                    .flatMap(pathItem -> pathItem.readOperations().stream())
                    .forEach(operation -> seenOperationIds.add(operation.getOperationId()));
        }

    }

}
