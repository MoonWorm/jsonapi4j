package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties.DefaultInfo;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.jsonApi4j;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.jsonApi4jWithoutOasPlugin;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.oasProperties;

class CommonOpenApiCustomizerTests {

    private static OpenAPI customised(OasProperties oasProperties) {
        return customised(jsonApi4j(oasProperties));
    }

    private static OpenAPI customised(JsonApi4j jsonApi4j) {
        OpenAPI openApi = new OpenAPI();
        new CommonOpenApiCustomizer(jsonApi4j).customise(openApi);
        return openApi;
    }

    @Nested
    class InfoExtensions {

        @Test
        void customise_extensionsConfigured_publishesThem() {
            DefaultOasProperties oasProperties = new DefaultOasProperties();
            DefaultInfo info = new DefaultInfo();
            info.setExtensions(Map.of("x-api-owner", "platform-team"));
            oasProperties.setInfo(info);

            assertThat(customised(oasProperties).getInfo().getExtensions())
                    .containsEntry("x-api-owner", "platform-team");
        }

        @Test
        void customise_noExtensionsConfigured_publishesNone() {
            DefaultOasProperties oasProperties = new DefaultOasProperties();
            oasProperties.setInfo(new DefaultInfo());

            assertThat(customised(oasProperties).getInfo().getExtensions()).isNull();
        }

    }

    @Nested
    class SecuritySchemes {

        @Test
        void customise_grantFlowsConfigured_declaresThemUnderTheConfiguredNames() {
            assertThat(customised(oasProperties("m2m", "user-facing")).getComponents().getSecuritySchemes())
                    .containsOnlyKeys("m2m", "user-facing");
        }

        @Test
        void customise_schemasAlreadyPresent_stillDeclaresSecuritySchemes() {
            JsonApi4j jsonApi4j = jsonApi4j(oasProperties("m2m", "user-facing"));

            // given a document whose components already carry schemas, as they do when this runs after the schema
            // customizers rather than first
            OpenAPI openApi = new OpenAPI();
            new JsonApiResponseSchemaCustomizer(jsonApi4j).customise(openApi);
            assertThat(openApi.getComponents().getSchemas()).isNotEmpty();

            // when
            new CommonOpenApiCustomizer(jsonApi4j).customise(openApi);

            // then
            assertThat(openApi.getComponents().getSecuritySchemes()).containsOnlyKeys("m2m", "user-facing");
        }

    }

    @Nested
    class AbsentConfig {

        @Test
        void customise_noOasPluginRegistered_stillPublishesTagsFromTheRegistries() {
            assertThat(customised(jsonApi4jWithoutOasPlugin()).getTags()).isNotEmpty();
        }

        @Test
        void customise_oasPluginRegistered_resolvesItsConfigFromThePluginRegistry() {
            assertThat(customised(oasProperties("m2m", "user-facing")).getComponents().getSecuritySchemes())
                    .containsOnlyKeys("m2m", "user-facing");
        }

    }

}
