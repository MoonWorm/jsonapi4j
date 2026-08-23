package pro.api4.jsonapi4j.plugin.ac;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlEntitlements;
import pro.api4.jsonapi4j.plugin.ac.annotation.EntitlementsGroup;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.entitlement.EntitlementsPolicy;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlEntitlementsModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlModel;
import pro.api4.jsonapi4j.plugin.ac.model.AccessControlScopesModel;
import pro.api4.jsonapi4j.principal.AuthenticatedPrincipalContextHolder;
import pro.api4.jsonapi4j.principal.DefaultPrincipal;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.ADMIN;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.NO_ACCESS;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.PARTNER;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.PUBLIC;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.ROOT_ADMIN;

class DefaultAccessControlEvaluatorTests {

    private static final String USER_ID = "user-42";

    private final DefaultAccessControlEvaluator sut = new DefaultAccessControlEvaluator();

    @AfterEach
    void clearPrincipal() {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(null);
    }

    private void givenPrincipalWithEntitlements(String... entitlements) {
        AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                new DefaultPrincipal(List.of(entitlements), Set.of(), USER_ID, Map.of()));
    }

    private boolean evaluateInbound(Class<?> annotatedResource) {
        AccessControlModel model = AccessControlModel.fromClassAnnotation(annotatedResource);
        return sut.evaluateInboundRequirements(new Object(), model);
    }

    @Nested
    class EntitlementsMatching {

        @Test
        void evaluateInboundRequirements_anyOfAndCallerHoldsOne_accessGranted() {
            givenPrincipalWithEntitlements(ADMIN);

            assertThat(evaluateInbound(AnyOfResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_anyOfAndCallerHoldsNone_accessDenied() {
            givenPrincipalWithEntitlements(PUBLIC, PARTNER);

            assertThat(evaluateInbound(AnyOfResource.class)).isFalse();
        }

        @Test
        void evaluateInboundRequirements_allOfAndCallerHoldsEvery_accessGranted() {
            givenPrincipalWithEntitlements(ADMIN, PARTNER);

            assertThat(evaluateInbound(AllOfResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_allOfAndCallerHoldsOnlySome_accessDenied() {
            givenPrincipalWithEntitlements(ADMIN);

            assertThat(evaluateInbound(AllOfResource.class)).isFalse();
        }

        @Test
        void evaluateInboundRequirements_allOfAndCallerHoldsBeyondRequired_accessGranted() {
            givenPrincipalWithEntitlements(ADMIN, PARTNER, PUBLIC);

            assertThat(evaluateInbound(AllOfResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_groupModeOmitted_evaluatedAsAnyOf() {
            givenPrincipalWithEntitlements(ROOT_ADMIN);

            assertThat(evaluateInbound(DefaultGroupModeResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_noneOfAndCallerHoldsNoneOfThem_accessGranted() {
            givenPrincipalWithEntitlements(ADMIN);

            assertThat(evaluateInbound(NoneOfResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_noneOfAndCallerHoldsOne_accessDenied() {
            givenPrincipalWithEntitlements(ADMIN, NO_ACCESS);

            assertThat(evaluateInbound(NoneOfResource.class)).isFalse();
        }

        @Test
        void evaluateInboundRequirements_noneOfAndCallerHoldsNothing_accessGranted() {
            // nothing held cannot include a forbidden entitlement, so NONE_OF is satisfied
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(List.of(), Set.of(), USER_ID, Map.of()));

            assertThat(evaluateInbound(NoneOfResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_singleEntitlementGroup_bothModesBehaveTheSame() {
            // given
            givenPrincipalWithEntitlements(ADMIN);

            // when - then
            assertThat(evaluateInbound(SingleEntitlementAnyOfResource.class)).isTrue();
            assertThat(evaluateInbound(SingleEntitlementAllOfResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_allOfClausesAndEveryClauseSatisfied_accessGranted() {
            givenPrincipalWithEntitlements(ADMIN, PARTNER, PUBLIC);

            assertThat(evaluateInbound(AllOfClausesResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_allOfClausesAndOneClauseUnsatisfied_accessDenied() {
            givenPrincipalWithEntitlements(ADMIN, PARTNER);

            assertThat(evaluateInbound(AllOfClausesResource.class)).isFalse();
        }

        @Test
        void evaluateInboundRequirements_anyOfClausesAndOneClauseSatisfied_accessGranted() {
            // fails the first clause outright, satisfies the second — an ALL_OF container would deny
            givenPrincipalWithEntitlements(PARTNER, ROOT_ADMIN);

            assertThat(evaluateInbound(AnyOfClausesResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_anyOfClausesAndNoClauseSatisfied_accessDenied() {
            givenPrincipalWithEntitlements(ADMIN, PARTNER);

            assertThat(evaluateInbound(AnyOfClausesResource.class)).isFalse();
        }

        @Test
        void evaluateInboundRequirements_policyDeclaredAndSatisfied_accessGranted() {
            givenPrincipalWithEntitlements(PARTNER);

            assertThat(evaluateInbound(PolicyResource.class)).isTrue();
        }

        @Test
        void evaluateInboundRequirements_policyDeclaredAndUnsatisfied_accessDenied() {
            givenPrincipalWithEntitlements(PUBLIC);

            assertThat(evaluateInbound(PolicyResource.class)).isFalse();
        }

        @Test
        void evaluateInboundRequirements_callerCarriesNoEntitlements_accessDenied() {
            // given
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(List.of(), Set.of(), USER_ID, Map.of()));

            // when - then
            assertThat(evaluateInbound(AnyOfResource.class)).isFalse();
            assertThat(evaluateInbound(AllOfResource.class)).isFalse();
        }

        @Test
        void evaluateInboundRequirements_noEntitlementsRequired_accessGranted() {
            givenPrincipalWithEntitlements(NO_ACCESS);

            assertThat(evaluateInbound(NoEntitlementsResource.class)).isTrue();
        }

    }

    @Nested
    class MissingEntitlementsDiagnostic {

        @Test
        void isMissingEntitlementsMisconfiguration_authenticatedPrincipalCarriesNoEntitlement_returnsTrue() {
            // given
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(null, Set.of("users.read"), USER_ID, Map.of()));

            // when - then
            assertThat(sut.isMissingEntitlementsMisconfiguration()).isTrue();
        }

        @Test
        void isMissingEntitlementsMisconfiguration_anonymousCaller_returnsFalse() {
            // given
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(null, Set.of(), null, Map.of()));

            // when - then
            assertThat(sut.isMissingEntitlementsMisconfiguration()).isFalse();
        }

        @Test
        void isMissingEntitlementsMisconfiguration_blankUserId_returnsFalse() {
            // given
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(null, Set.of(), "   ", Map.of()));

            // when - then
            assertThat(sut.isMissingEntitlementsMisconfiguration()).isFalse();
        }

        @Test
        void isMissingEntitlementsMisconfiguration_principalHoldsADifferentEntitlement_returnsFalse() {
            givenPrincipalWithEntitlements(PUBLIC);

            assertThat(sut.isMissingEntitlementsMisconfiguration()).isFalse();
        }

        @Test
        void isMissingEntitlementsMisconfiguration_principalHoldsASufficientEntitlement_returnsFalse() {
            givenPrincipalWithEntitlements(ADMIN);

            assertThat(sut.isMissingEntitlementsMisconfiguration()).isFalse();
        }

        @Test
        void isMissingEntitlementsMisconfiguration_noPrincipalIsSet_returnsFalse() {
            assertThat(sut.isMissingEntitlementsMisconfiguration()).isFalse();
        }

    }

    @Nested
    class MissingScopesDiagnostic {

        @Test
        void isMissingScopesMisconfiguration_authenticatedPrincipalCarriesNoScopes_returnsTrue() {
            // given
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(List.of(ADMIN), Set.of(), USER_ID, Map.of()));

            // when - then
            assertThat(sut.isMissingScopesMisconfiguration()).isTrue();
        }

        @Test
        void isMissingScopesMisconfiguration_anonymousCaller_returnsFalse() {
            // given
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(null, Set.of(), null, Map.of()));

            // when - then
            assertThat(sut.isMissingScopesMisconfiguration()).isFalse();
        }

        @Test
        void isMissingScopesMisconfiguration_blankUserId_returnsFalse() {
            // given
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(null, Set.of(), "   ", Map.of()));

            // when - then
            assertThat(sut.isMissingScopesMisconfiguration()).isFalse();
        }

        @Test
        void isMissingScopesMisconfiguration_principalHoldsDifferentScopes_returnsFalse() {
            // given
            AuthenticatedPrincipalContextHolder.setAuthenticatedPrincipalContext(
                    new DefaultPrincipal(null, Set.of("users.read"), USER_ID, Map.of()));

            // when - then
            assertThat(sut.isMissingScopesMisconfiguration()).isFalse();
        }

        @Test
        void isMissingScopesMisconfiguration_noPrincipalIsSet_returnsFalse() {
            assertThat(sut.isMissingScopesMisconfiguration()).isFalse();
        }

    }

    @Nested
    class DenialDescription {

        @Test
        void describeRequirement_singleClause_rendersContainerModeAndClause() {
            assertThat(DefaultAccessControlEvaluator.describeRequirement(requirementOf(AllOfResource.class)))
                    .isEqualTo("ALL_OF[ALL_OF[ADMIN, PARTNER]]");
        }

        @Test
        void describeRequirement_severalClauses_rendersEveryClause() {
            assertThat(DefaultAccessControlEvaluator.describeRequirement(requirementOf(AnyOfClausesResource.class)))
                    .isEqualTo("ANY_OF[ALL_OF[ADMIN, PUBLIC], ALL_OF[PARTNER, ROOT_ADMIN]]");
        }

        @Test
        void describeRequirement_descriptionDeclared_quotedAheadOfStructure() {
            assertThat(DefaultAccessControlEvaluator.describeRequirement(requirementOf(DescribedResource.class)))
                    .isEqualTo("'internal partner tooling' (ALL_OF[ANY_OF[ADMIN]])");
        }

        @Test
        void describeRequirement_policyDeclared_namesThePolicyClass() {
            assertThat(DefaultAccessControlEvaluator.describeRequirement(requirementOf(PolicyResource.class)))
                    .contains("policy")
                    .contains("AdminOrPartnerPolicy");
        }

        @Test
        void describeRequirement_entitlementsDifferingOnlyByTypo_bothRenderedInFull() {
            assertThat(DefaultAccessControlEvaluator.describeRequirement(requirementOf(TypoEntitlementResource.class)))
                    .isEqualTo("ALL_OF[ANY_OF[ADMNI]]")
                    .isNotEqualTo(DefaultAccessControlEvaluator.describeRequirement(
                            requirementOf(SingleEntitlementAnyOfResource.class)));
        }

        @Test
        void describeScopesRequirement_scopesList_rendersSortedScopes() {
            AccessControlScopesModel requirement = AccessControlModel
                    .fromClassAnnotation(ScopesListResource.class)
                    .getRequiredScopes();

            assertThat(DefaultAccessControlEvaluator.describeScopesRequirement(requirement))
                    .isEqualTo("scopes [users.read, users.write]");
        }

        @Test
        void describeScopesRequirement_scopesExpression_rendersExpression() {
            AccessControlScopesModel requirement = AccessControlModel
                    .fromClassAnnotation(ScopesExpressionResource.class)
                    .getRequiredScopes();

            assertThat(DefaultAccessControlEvaluator.describeScopesRequirement(requirement))
                    .isEqualTo("scopes expression 'users.read AND users.write'");
        }

        private AccessControlEntitlementsModel requirementOf(Class<?> annotatedResource) {
            return AccessControlModel.fromClassAnnotation(annotatedResource).getRequiredEntitlements();
        }

    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            @EntitlementsGroup(value = {ADMIN, ROOT_ADMIN}, mode = EntitlementsGroup.Mode.ANY_OF)))
    private static class AnyOfResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            @EntitlementsGroup(value = {ADMIN, PARTNER}, mode = EntitlementsGroup.Mode.ALL_OF)))
    private static class AllOfResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            @EntitlementsGroup(value = {ADMIN, ROOT_ADMIN})))
    private static class DefaultGroupModeResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            @EntitlementsGroup(value = NO_ACCESS, mode = EntitlementsGroup.Mode.NONE_OF)))
    private static class NoneOfResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            @EntitlementsGroup(value = ADMIN, mode = EntitlementsGroup.Mode.ANY_OF)))
    private static class SingleEntitlementAnyOfResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            @EntitlementsGroup(value = ADMIN, mode = EntitlementsGroup.Mode.ALL_OF)))
    private static class SingleEntitlementAllOfResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            mode = AccessControlEntitlements.Mode.ALL_OF,
            value = {
                    @EntitlementsGroup(value = {ADMIN, ROOT_ADMIN}, mode = EntitlementsGroup.Mode.ANY_OF),
                    @EntitlementsGroup(value = {PARTNER, PUBLIC}, mode = EntitlementsGroup.Mode.ALL_OF)
            }))
    private static class AllOfClausesResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            mode = AccessControlEntitlements.Mode.ANY_OF,
            value = {
                    @EntitlementsGroup(value = {ADMIN, PUBLIC}, mode = EntitlementsGroup.Mode.ALL_OF),
                    @EntitlementsGroup(value = {PARTNER, ROOT_ADMIN}, mode = EntitlementsGroup.Mode.ALL_OF)
            }))
    private static class AnyOfClausesResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            description = "internal partner tooling",
            value = @EntitlementsGroup(ADMIN)))
    private static class DescribedResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(policy = AdminOrPartnerPolicy.class))
    private static class PolicyResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(@EntitlementsGroup("ADMNI")))
    private static class TypoEntitlementResource {
    }

    public static class AdminOrPartnerPolicy implements EntitlementsPolicy {
        @Override
        public boolean isSatisfiedBy(List<String> entitlements) {
            return entitlements.contains(ADMIN) || entitlements.contains(PARTNER);
        }
    }

    @AccessControl(scopes = @AccessControlScopes(requiredScopes = {"users.write", "users.read"}))
    private static class ScopesListResource {
    }

    @AccessControl(scopes = @AccessControlScopes(requiredScopesExpression = "users.read AND users.write"))
    private static class ScopesExpressionResource {
    }

    @AccessControl
    private static class NoEntitlementsResource {
    }

}
