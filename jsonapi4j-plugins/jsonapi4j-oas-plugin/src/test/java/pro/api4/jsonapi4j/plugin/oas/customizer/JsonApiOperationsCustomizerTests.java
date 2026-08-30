package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.CustomPayloadOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.DescribedOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredAttributes;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.WriteOperations;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasCustomizerTestFixtures.build;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.CUSTOM_DESCRIPTION;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.CUSTOM_SUMMARY;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SCOPE;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SECURED_RESOURCE_TYPE;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.jsonApi4j;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.oasProperties;

/**
 * Two guarantees this customizer owes the generated document: the reserved meta types registered by
 * {@code jsonapi4j.meta.enabled=true} never get operation paths (the customizer derives that partition from
 * {@link DomainRegistry#getMetaResourceTypes()}), and every operation-level security requirement names a scheme that
 * {@link CommonOpenApiCustomizer} actually declares under {@code components.securitySchemes} — an undeclared name is a
 * dangling reference that silently breaks the Swagger UI authorize button.
 */
class JsonApiOperationsCustomizerTests {

    private static final String ROOT_PATH = "/jsonapi";

    private static Operation securedOperation(OpenAPI openApi) {
        return openApi.getPaths().get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE + "/{id}").getGet();
    }

    private static Paths documentedPaths(ResourceOperations<SecuredAttributes> operations) {
        JsonApi4j jsonApi4j = jsonApi4j(new DefaultOasProperties(), operations);

        OpenAPI openApi = new OpenAPI();
        JsonApiOperationsCustomizer sut = new JsonApiOperationsCustomizer(
                ROOT_PATH,
                jsonApi4j.getDomainRegistry(),
                jsonApi4j.getOperationsRegistry(),
                null
        );
        sut.customise(openApi);

        return openApi.getPaths();
    }

    @Nested
    class MetaTypes {

        @Test
        void customise_metaEnabledRegistry_documentsNoPaths() {
            JsonApi4j withMeta = build(true);
            assertThat(withMeta.getDomainRegistry().isMetaEnabled()).isTrue();

            OpenAPI openApi = new OpenAPI();
            JsonApiOperationsCustomizer sut = new JsonApiOperationsCustomizer(
                    ROOT_PATH,
                    withMeta.getDomainRegistry(),
                    withMeta.getOperationsRegistry(),
                    null
            );
            sut.customise(openApi);

            Set<String> paths = openApi.getPaths() == null ? Set.of() : openApi.getPaths().keySet();
            assertThat(paths).isEmpty();
        }

    }

    @Nested
    class SummaryAndDescription {

        @Test
        void customise_annotationOverridesDeclared_preferThemOverGeneratedText() {
            Operation operation = describedOperation(new DescribedOperations());

            assertThat(operation.getSummary()).isEqualTo(CUSTOM_SUMMARY);
            assertThat(operation.getDescription()).isEqualTo(CUSTOM_DESCRIPTION);
        }

        @Test
        void customise_noAnnotationOverrides_fallBackToGeneratedText() {
            Operation operation = describedOperation(new SecuredOperations());

            assertThat(operation.getSummary()).isEqualTo("Get single secured");
            assertThat(operation.getDescription()).isEqualTo("Retrieves secured details by resource id.");
        }

        private Operation describedOperation(ResourceOperations<SecuredAttributes> operations) {
            JsonApi4j jsonApi4j = jsonApi4j(new DefaultOasProperties(), operations);

            OpenAPI openApi = new OpenAPI();
            JsonApiOperationsCustomizer sut = new JsonApiOperationsCustomizer(
                    ROOT_PATH,
                    jsonApi4j.getDomainRegistry(),
                    jsonApi4j.getOperationsRegistry(),
                    null
            );
            sut.customise(openApi);

            return securedOperation(openApi);
        }

    }

    @Nested
    class RequestBodies {

        @Test
        void customise_createOperation_documentsDerivedRequestBody() {
            PathItem pathItem = writeOperationPaths().get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE);

            assertThat(bodySchemaRefOf(pathItem.getPost())).isEqualTo("#/components/schemas/SecuredCreateRequestDoc");
            assertThat(pathItem.getPost().getRequestBody().getRequired()).isTrue();
        }

        @Test
        void customise_updateOperation_documentsDerivedRequestBody() {
            PathItem pathItem = writeOperationPaths().get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE + "/{id}");

            assertThat(bodySchemaRefOf(pathItem.getPatch())).isEqualTo("#/components/schemas/SecuredUpdateRequestDoc");
        }

        @Test
        void customise_deleteResourceOperation_documentsNoRequestBody() {
            PathItem pathItem = writeOperationPaths().get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE + "/{id}");

            assertThat(pathItem.getDelete().getRequestBody()).isNull();
        }

        @Test
        void customise_payloadTypeDeclared_overridesDerivedRequestBody() {
            Paths paths = documentedPaths(new CustomPayloadOperations());

            assertThat(bodySchemaRefOf(paths.get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE).getPost()))
                    .isEqualTo("#/components/schemas/CustomPayload");
        }

        private Paths writeOperationPaths() {
            return documentedPaths(new WriteOperations());
        }

        private String bodySchemaRefOf(Operation operation) {
            return operation.getRequestBody()
                    .getContent()
                    .get(JsonApiMediaType.MEDIA_TYPE)
                    .getSchema()
                    .get$ref();
        }

    }

    @Nested
    class SecurityRequirements {

        @Test
        void customise_grantFlowsConfigured_namesSchemesFromConfig() {
            List<SecurityRequirement> securityRequirements = securityRequirementsOf(oasProperties("m2m", "user-facing"));

            assertThat(securityRequirements).hasSize(2);
            assertThat(securityRequirements.get(0)).containsOnlyKeys("m2m");
            assertThat(securityRequirements.get(1)).containsOnlyKeys("user-facing");
        }

        @Test
        void customise_requiredScopesDeclared_appliesThemToEveryGrantFlow() {
            List<SecurityRequirement> securityRequirements = securityRequirementsOf(oasProperties("m2m", "user-facing"));

            assertThat(securityRequirements)
                    .allSatisfy(requirement -> assertThat(requirement.values()).containsExactly(List.of(SCOPE)));
        }

        @Test
        void customise_grantFlowsConfigured_namesOnlyDeclaredSecuritySchemes() {
            // given
            DefaultOasProperties oasProperties = oasProperties("m2m", "user-facing");
            JsonApi4j jsonApi4j = jsonApi4j(oasProperties);
            OpenAPI openApi = new OpenAPI();

            // when
            new CommonOpenApiCustomizer(
                    oasProperties,
                    jsonApi4j.getDomainRegistry(),
                    jsonApi4j.getOperationsRegistry()
            ).customise(openApi);
            new JsonApiOperationsCustomizer(
                    ROOT_PATH,
                    jsonApi4j.getDomainRegistry(),
                    jsonApi4j.getOperationsRegistry(),
                    oasProperties
            ).customise(openApi);

            // then
            assertThat(securedOperation(openApi).getSecurity())
                    .isNotEmpty()
                    .allSatisfy(requirement -> assertThat(openApi.getComponents().getSecuritySchemes())
                            .containsKeys(requirement.keySet().toArray(new String[0])));
        }

        @Test
        void customise_pkceNotConfigured_skipsPkceRequirement() {
            List<SecurityRequirement> securityRequirements = securityRequirementsOf(oasProperties("m2m", null));

            assertThat(securityRequirements).hasSize(1);
            assertThat(securityRequirements.get(0)).containsOnlyKeys("m2m");
        }

        @Test
        void customise_clientCredentialsNotConfigured_skipsClientCredentialsRequirement() {
            List<SecurityRequirement> securityRequirements = securityRequirementsOf(oasProperties(null, "user-facing"));

            assertThat(securityRequirements).hasSize(1);
            assertThat(securityRequirements.get(0)).containsOnlyKeys("user-facing");
        }

        @Test
        void customise_noGrantFlowConfigured_documentsNoSecurity() {
            assertThat(securityRequirementsOf(oasProperties(null, null))).isNull();
        }

        @Test
        void customise_noOasProperties_documentsNoSecurity() {
            assertThat(securityRequirementsOf(null)).isNull();
        }

        private List<SecurityRequirement> securityRequirementsOf(OasProperties oasProperties) {
            JsonApi4j jsonApi4j = jsonApi4j(oasProperties == null ? new DefaultOasProperties() : oasProperties);

            OpenAPI openApi = new OpenAPI();
            JsonApiOperationsCustomizer sut = new JsonApiOperationsCustomizer(
                    ROOT_PATH,
                    jsonApi4j.getDomainRegistry(),
                    jsonApi4j.getOperationsRegistry(),
                    oasProperties
            );
            sut.customise(openApi);

            return securedOperation(openApi).getSecurity();
        }

    }

}
