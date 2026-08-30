package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.CustomPayloadOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredAttributes;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.WriteOperations;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.jsonApi4j;

/**
 * A request document is not a response document: {@code id} is optional on create and mandatory on update, and neither
 * {@code links} nor {@code included} may appear. These tests pin that separation, and that a declared
 * {@code payloadType} still gets its schema registered.
 */
class JsonApiRequestBodySchemaCustomizerTests {

    private static Map<String, Schema> schemasOf(ResourceOperations<SecuredAttributes> operations) {
        JsonApi4j jsonApi4j = jsonApi4j(new DefaultOasProperties(), operations);

        OpenAPI openApi = new OpenAPI();
        JsonApiRequestBodySchemaCustomizer sut = new JsonApiRequestBodySchemaCustomizer(
                jsonApi4j.getDomainRegistry(),
                jsonApi4j.getOperationsRegistry()
        );
        sut.customise(openApi);

        return openApi.getComponents() == null ? Map.of() : openApi.getComponents().getSchemas();
    }

    @Nested
    class ResourceRequestSchemas {

        @Test
        void customise_createConfigured_leavesIdOptional() {
            Schema createResource = schemasOf(new WriteOperations()).get("SecuredCreateResource");

            assertThat(createResource.getRequired()).containsExactly("type");
            assertThat(createResource.getProperties()).containsKey("id");
        }

        @Test
        void customise_updateConfigured_requiresId() {
            Schema updateResource = schemasOf(new WriteOperations()).get("SecuredUpdateResource");

            assertThat(updateResource.getRequired()).containsExactlyInAnyOrder("id", "type");
        }

        @Test
        void customise_writeConfigured_omitsResponseOnlyMembers() {
            Map<String, Schema> schemas = schemasOf(new WriteOperations());

            assertThat(schemas.get("SecuredCreateResource").getProperties()).doesNotContainKey("links");
            assertThat(schemas.get("SecuredCreateRequestDoc").getProperties()).containsOnlyKeys("data");
            assertThat(schemas.get("SecuredUpdateRequestDoc").getProperties()).containsOnlyKeys("data");
        }

        @Test
        void customise_noWriteOperationConfigured_registersNoRequestSchemas() {
            assertThat(schemasOf(new SecuredOperations()).keySet())
                    .noneSatisfy(name -> assertThat(name).contains("Request"));
        }

    }

    @Nested
    class CustomPayloadSchemas {

        @Test
        void customise_payloadTypeDeclared_registersItsSchema() {
            assertThat(schemasOf(new CustomPayloadOperations())).containsKey("CustomPayload");
        }

        @Test
        void customise_noPayloadTypeDeclared_registersNoCustomSchema() {
            assertThat(schemasOf(new WriteOperations())).doesNotContainKey("CustomPayload");
        }

    }

}
