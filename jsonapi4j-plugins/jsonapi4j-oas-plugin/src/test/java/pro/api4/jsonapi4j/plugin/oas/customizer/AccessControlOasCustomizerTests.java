package pro.api4.jsonapi4j.plugin.oas.customizer;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.JsonApi4j;
import pro.api4.jsonapi4j.operation.ResourceOperations;
import pro.api4.jsonapi4j.plugin.oas.config.DiagnosticsMode;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.BothScopesOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.CrossProductOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.DeclaredScopeOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.GuardedAttributes;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.GuardedOperations;
import pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.UndeclaredScopeOperations;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.ADMIN;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.ENTITLEMENTS_REASON;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.GUARDED;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.PKCE_SCHEME;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.READ;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.SCHEME;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.SCOPES_REASON;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.WRITE;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.allScopes;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.jsonApi4j;
import static pro.api4.jsonapi4j.plugin.oas.customizer.OasAccessControlTestFixtures.twoFlowJsonApi4j;

/**
 * What access control enforces is what the document should state: which scopes an operation needs, which operations
 * anyone may call, and where a {@code 403} is reachable. A denied read is answered with an empty document and a
 * {@code 200}, deliberately, so reads never carry one.
 */
class AccessControlOasCustomizerTests {

    private static final String ROOT = "/jsonapi/" + GUARDED;

    private static OpenAPI document(ResourceOperations<GuardedAttributes> operations,
                                    String... declaredScopes) {
        return document(jsonApi4j(operations, declaredScopes));
    }

    private static OpenAPI document(JsonApi4j jsonApi4j) {
        OpenAPI openApi = new OpenAPI();
        new JsonApiOperationsCustomizer(jsonApi4j).customise(openApi);
        new AccessControlOasCustomizer(jsonApi4j).customise(openApi);
        return openApi;
    }

    private static OpenAPI guardedDocument() {
        return document(new GuardedOperations(), allScopes().toArray(new String[0]));
    }

    private static Operation readById(OpenAPI openApi) {
        return openApi.getPaths().get(ROOT + "/{id}").getGet();
    }

    @Nested
    class ScopeRequirements {

        @Test
        void customise_anyOfClauses_becomeAlternativeSecurityRequirements() {
            List<SecurityRequirement> security = readById(guardedDocument()).getSecurity();

            assertThat(security).hasSize(2);
            assertThat(security.get(0).get(SCHEME)).containsExactly(READ, WRITE);
            assertThat(security.get(1).get(SCHEME)).containsExactly(ADMIN);
        }

        @Test
        void customise_allOfOverAnyOfClauses_expandsIntoEveryCombination() {
            List<SecurityRequirement> security = readById(
                    document(new CrossProductOperations(), allScopes().toArray(new String[0]))).getSecurity();

            assertThat(security).hasSize(4);
            assertThat(security).extracting(requirement -> requirement.get(SCHEME))
                    .containsExactlyInAnyOrder(
                            List.of(READ, ADMIN),
                            List.of(READ),
                            List.of(WRITE, ADMIN),
                            List.of(WRITE, READ)
                    );
        }

        /**
         * {@code (READ or WRITE) and (ADMIN or READ)} has a branch where both clauses are satisfied by the same
         * scope. An alternative is a set, so that branch asks for one scope rather than naming it twice.
         */
        @Test
        void customise_combinationSatisfiedByOneScope_asksForItOnce() {
            List<SecurityRequirement> security = readById(
                    document(new CrossProductOperations(), allScopes().toArray(new String[0]))).getSecurity();

            assertThat(security).anySatisfy(requirement -> assertThat(requirement.get(SCHEME)).containsExactly(READ));
        }

        @Test
        void customise_scopeTheConfigurationDoesNotDeclare_failsRatherThanDangle() {
            assertThatThrownBy(() -> document(new UndeclaredScopeOperations(), allScopes().toArray(new String[0])))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("never.declared");
        }

    }

    /**
     * OpenAPI reads an operation's scopes against the scheme it names, so a requirement belongs only on the flows
     * that declare its scopes. Hanging every scope off every configured flow is what publishes a scope Swagger UI's
     * authorize dialog cannot offer and a linter rejects.
     */
    @Nested
    class ScopesMatchTheirOwnScheme {

        private OpenAPI splitDocument(ResourceOperations<GuardedAttributes> operations,
                                      List<String> clientCredentialsScopes,
                                      List<String> pkceScopes) {
            return document(twoFlowJsonApi4j(operations, clientCredentialsScopes, pkceScopes));
        }

        @Test
        void customise_scopesSplitBetweenFlows_hangsEachOnlyOnTheFlowDeclaringIt() {
            List<SecurityRequirement> security = readById(splitDocument(
                    new GuardedOperations(), List.of(ADMIN), List.of(READ, WRITE))).getSecurity();

            assertThat(security).hasSize(2);
            assertThat(security.get(0)).containsOnlyKeys(SCHEME);
            assertThat(security.get(0).get(SCHEME)).containsExactly(ADMIN);
            assertThat(security.get(1)).containsOnlyKeys(PKCE_SCHEME);
            assertThat(security.get(1).get(PKCE_SCHEME)).containsExactly(READ, WRITE);
        }

        @Test
        void customise_flowDeclaringNoScopes_carriesNoScopedRequirement() {
            List<SecurityRequirement> security = readById(splitDocument(
                    new GuardedOperations(), List.of(), allScopes())).getSecurity();

            assertThat(security).allSatisfy(requirement -> assertThat(requirement).doesNotContainKey(SCHEME));
        }

        @Test
        void customise_everyPublishedScope_isDeclaredByTheSchemeNamingIt() {
            OpenAPI openApi = splitDocument(new GuardedOperations(), List.of(ADMIN), List.of(READ, WRITE));
            Map<String, List<String>> declared = Map.of(SCHEME, List.of(ADMIN), PKCE_SCHEME, List.of(READ, WRITE));

            assertThat(readById(openApi).getSecurity()).allSatisfy(requirement -> requirement
                    .forEach((scheme, scopes) -> assertThat(declared.get(scheme)).containsAll(scopes)));
        }

        /**
         * Neither flow declares both scopes, so the requirement goes unstated rather than being hung off a flow that
         * would reject it. The operation then falls back to the document's own security, which is the weaker but
         * truthful answer - and the diagnostic says so.
         */
        @Test
        void customise_requirementNoFlowCanCarry_isLeftOutRatherThanDangled() {
            List<SecurityRequirement> security = readById(splitDocument(
                    new BothScopesOperations(), List.of(READ), List.of(WRITE))).getSecurity();

            assertThat(security).isNullOrEmpty();
        }

        @Test
        void customise_requirementNoFlowCanCarry_failsWhenConfiguredToFail() {
            JsonApi4j jsonApi4j = twoFlowJsonApi4j(new BothScopesOperations(), List.of(READ), List.of(WRITE),
                    DiagnosticsMode.FAIL_ON_REQUEST);

            assertThatThrownBy(() -> document(jsonApi4j))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining(READ)
                    .hasMessageContaining(WRITE);
        }

        /**
         * The same rule on the other path a scope reaches the document by - declared on the operation rather than
         * derived from what access control enforces.
         */
        @Test
        void customise_scopesDeclaredOnTheOperation_hangOnlyOnTheFlowDeclaringThem() {
            List<SecurityRequirement> security = readById(splitDocument(
                    new DeclaredScopeOperations(), List.of(ADMIN), List.of(READ))).getSecurity();

            assertThat(security).hasSize(1);
            assertThat(security.get(0)).containsOnlyKeys(PKCE_SCHEME);
            assertThat(security.get(0).get(PKCE_SCHEME)).containsExactly(READ);
        }

    }

    @Nested
    class AnonymousOperations {

        @Test
        void customise_anonymousOperation_isDocumentedAsNeedingNoAuthentication() {
            assertThat(guardedDocument().getPaths().get(ROOT + "/{id}").getDelete().getSecurity()).isEmpty();
        }

        @Test
        void customise_anonymousOperation_documentsNo403() {
            assertThat(guardedDocument().getPaths().get(ROOT + "/{id}").getDelete().getResponses())
                    .doesNotContainKey("403");
        }

    }

    @Nested
    class Forbidden {

        @Test
        void customise_guardedWrite_documents403() {
            assertThat(guardedDocument().getPaths().get(ROOT).getPost().getResponses()).containsKey("403");
        }

        @Test
        void customise_guardedRead_documentsNo403() {
            assertThat(readById(guardedDocument()).getResponses()).doesNotContainKey("403");
        }

        @Test
        void customise_operationWithoutRequirements_documentsNo403() {
            assertThat(guardedDocument().getPaths().get(ROOT + "/{id}").getPatch().getResponses())
                    .doesNotContainKey("403");
        }

    }

    @Nested
    class Reasons {

        @Test
        void customise_requirementWithDescription_explainsItInTheOperation() {
            assertThat(readById(guardedDocument()).getDescription()).contains(SCOPES_REASON);
        }

        @Test
        void customise_entitlementsRequirement_isExplainedEvenThoughOpenApiCannotStateIt() {
            assertThat(guardedDocument().getPaths().get(ROOT).getPost().getDescription())
                    .contains(ENTITLEMENTS_REASON);
        }

    }

}
