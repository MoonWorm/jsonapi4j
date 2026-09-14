package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.PluginRegistry;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.PeerPlugin;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Access control refuses a write with {@code 403} but answers a denied read with an empty document and a {@code 200},
 * deliberately, so that the compound-documents resolver can keep going. The document has to say the same.
 */
class AccessControlOasCustomizerTests {

    private static final String AC = "JsonApiAccessControlPlugin";

    private static OpenAPI document() {
        Operation read = new Operation().responses(new ApiResponses()
                .addApiResponse("200", new ApiResponse().description("OK")));
        Operation write = new Operation().responses(new ApiResponses()
                .addApiResponse("204", new ApiResponse().description("No Content")));
        return new OpenAPI().paths(new Paths()
                .addPathItem("/things", new PathItem().get(read).post(write)));
    }

    private static AccessControlOasCustomizer sut(String... activePlugins) {
        PluginRegistry.PluginRegistryBuilder builder = PluginRegistry.builder();
        for (String name : activePlugins) {
            builder.register(new PeerPlugin(name));
        }
        PluginRegistry registry = builder.build();
        return new AccessControlOasCustomizer(() -> registry);
    }

    @Nested
    class ForbiddenOnWrites {

        @Test
        void customise_accessControlActive_documents403OnWrites() {
            OpenAPI openApi = document();

            sut(AC).customise(openApi);

            assertThat(openApi.getPaths().get("/things").getPost().getResponses()).containsKey("403");
        }

        @Test
        void customise_accessControlActive_leavesReadsAlone() {
            OpenAPI openApi = document();

            sut(AC).customise(openApi);

            assertThat(openApi.getPaths().get("/things").getGet().getResponses()).doesNotContainKey("403");
        }

        @Test
        void customise_accessControlAbsent_documentsNothing() {
            OpenAPI openApi = document();

            sut().customise(openApi);

            assertThat(openApi.getPaths().get("/things").getPost().getResponses()).doesNotContainKey("403");
        }

        @Test
        void customise_operationAlreadyDocumenting403_leavesItAsItIs() {
            OpenAPI openApi = document();
            ApiResponse declared = new ApiResponse().description("Client-generated id is not supported");
            openApi.getPaths().get("/things").getPost().getResponses().addApiResponse("403", declared);

            sut(AC).customise(openApi);

            assertThat(openApi.getPaths().get("/things").getPost().getResponses().get("403")).isSameAs(declared);
        }

    }

}
