package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.domain.DomainRegistry;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.plugin.oas.config.DefaultOasProperties;
import pro.api4.jsonapi4j.plugin.oas.config.OasProperties;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.CursorOnlyListingOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.CustomPayloadOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.ListingOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.DescribedOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredAttributes;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.SecuredOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasOperationTestFixtures.WriteOperations;
import pro.api4.jsonapi4j.request.JsonApiMediaType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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

    private static Paths writeOperationPaths() {
        return documentedPaths(new WriteOperations());
    }

    private static Paths documentedPaths(ResourceOperations<SecuredAttributes> operations) {
        JsonApi4j jsonApi4j = jsonApi4j(new DefaultOasProperties(), operations);

        OpenAPI openApi = new OpenAPI();
        JsonApiOperationsCustomizer sut = new JsonApiOperationsCustomizer(jsonApi4j);
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
            JsonApiOperationsCustomizer sut = new JsonApiOperationsCustomizer(withMeta);
            sut.customise(openApi);

            Set<String> paths = openApi.getPaths() == null ? Set.of() : openApi.getPaths().keySet();
            assertThat(paths).isEmpty();
        }

    }

    /**
     * The framework parses {@code sort} and both pagination styles for every request, but only the operation can act
     * on them — so they are published from what the operation declared, never from what the request layer accepts.
     */
    @Nested
    class DeclaredQueryParameters {

        @Test
        void customise_sortableFieldsDeclared_publishesSortWithBothDirections() {
            Parameter sort = collectionParam(new ListingOperations(), "sort");

            List<Object> allowedValues = new ArrayList<>(((ArraySchema) sort.getSchema()).getItems().getEnum());

            assertThat(allowedValues).containsExactly("id", "-id", "createdAt", "-createdAt");
        }

        @Test
        void customise_noSortableFieldsDeclared_publishesNoSort() {
            assertThat(collectionParamNames(new CursorOnlyListingOperations())).doesNotContain("sort");
        }

        @Test
        void customise_limitOffsetDeclared_publishesBothPageParams() {
            assertThat(collectionParamNames(new ListingOperations()))
                    .contains("page[cursor]", "page[limit]", "page[offset]");
        }

        @Test
        void customise_paginationNotDeclared_publishesCursorOnly() {
            assertThat(collectionParamNames(new CursorOnlyListingOperations()))
                    .contains("page[cursor]")
                    .doesNotContain("page[limit]", "page[offset]");
        }

        @Test
        void customise_limitParam_carriesItsDefaultAndBounds() {
            Parameter limit = collectionParam(new ListingOperations(), "page[limit]");

            // IntegerSchema narrows the long default to an int when it takes it
            assertThat(limit.getSchema().getDefault()).isEqualTo(20);
            assertThat(limit.getSchema().getMinimum()).isEqualTo(BigDecimal.ONE);
        }

        private Set<String> collectionParamNames(ResourceOperations<SecuredAttributes> operations) {
            return collectionParams(operations).stream().map(Parameter::getName).collect(Collectors.toSet());
        }

        private Parameter collectionParam(ResourceOperations<SecuredAttributes> operations,
                                          String name) {
            return collectionParams(operations).stream()
                    .filter(parameter -> name.equals(parameter.getName()))
                    .findFirst()
                    .orElseThrow(() -> new AssertionError("no '" + name + "' parameter was published"));
        }

        private List<Parameter> collectionParams(ResourceOperations<SecuredAttributes> operations) {
            return documentedPaths(operations).get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE).getGet().getParameters();
        }

    }

    /**
     * Client generators name their methods after {@code operationId}. Without it they fall back to inventing one from
     * the path and verb, which makes every generated client break the moment a path changes.
     */
    @Nested
    class OperationIds {

        @Test
        void customise_everyOperation_getsAnOperationId() {
            assertThat(writeOperationPaths().values())
                    .flatMap(PathItem::readOperations)
                    .allSatisfy(operation -> assertThat(operation.getOperationId()).isNotBlank());
        }

        @Test
        void customise_operations_getDistinctOperationIds() {
            List<String> operationIds = writeOperationPaths().values().stream()
                    .flatMap(pathItem -> pathItem.readOperations().stream())
                    .map(Operation::getOperationId)
                    .toList();

            assertThat(operationIds).doesNotHaveDuplicates();
        }

        @Test
        void customise_operation_namesItAfterTheOperationAndResource() {
            Paths paths = writeOperationPaths();

            assertThat(paths.get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE).getPost().getOperationId())
                    .isEqualTo("create-single-secured");
            assertThat(paths.get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE + "/{id}").getDelete().getOperationId())
                    .isEqualTo("delete-single-secured");
        }

    }

    /**
     * An operation whose only documented outcomes are failures is incomplete as a contract and useless to a client
     * generator, so a success response is published even when the operation answers with no body.
     */
    @Nested
    class SuccessResponses {

        @Test
        void customise_operationReturningNoBody_documentsItsSuccessStatus() {
            ApiResponse response = successResponseOf(writeOperationPaths()
                    .get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE + "/{id}").getPatch(), "204");

            assertThat(response.getDescription()).startsWith("No Content.");
            assertThat(response.getContent()).isNull();
        }

        @Test
        void customise_operationReturningABody_documentsStatusAndContent() {
            ApiResponse response = successResponseOf(writeOperationPaths()
                    .get(ROOT_PATH + "/" + SECURED_RESOURCE_TYPE).getPost(), "201");

            assertThat(response.getDescription()).startsWith("Created.");
            assertThat(response.getContent().get(JsonApiMediaType.MEDIA_TYPE).getSchema().get$ref())
                    .isEqualTo("#/components/schemas/SecuredSingleResourceDoc");
        }

        @Test
        void customise_everyOperation_documentsAtLeastOneSuccessResponse() {
            assertThat(writeOperationPaths().values())
                    .flatMap(PathItem::readOperations)
                    .allSatisfy(operation -> assertThat(operation.getResponses().keySet())
                            .anySatisfy(status -> assertThat(status).startsWith("2")));
        }

        private ApiResponse successResponseOf(Operation operation,
                                              String status) {
            return operation.getResponses().get(status);
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
            JsonApiOperationsCustomizer sut = new JsonApiOperationsCustomizer(jsonApi4j);
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
            new CommonOpenApiCustomizer(jsonApi4j).customise(openApi);
            new JsonApiOperationsCustomizer(jsonApi4j).customise(openApi);

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
            JsonApiOperationsCustomizer sut = new JsonApiOperationsCustomizer(jsonApi4j);
            sut.customise(openApi);

            return securedOperation(openApi).getSecurity();
        }

    }

}
