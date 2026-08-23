package pro.api4.jsonapi4j.plugin.ac.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlEntitlements;
import pro.api4.jsonapi4j.plugin.ac.annotation.EntitlementsGroup;
import pro.api4.jsonapi4j.plugin.ac.entitlement.EntitlementsPolicy;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessControlEntitlementsModelTests {

    private static AccessControlEntitlements entitlementsOf(Class<?> annotatedType) {
        return annotatedType.getAnnotation(AccessControl.class).entitlements();
    }

    private static AccessControlEntitlementsModel modelOf(Class<?> annotatedType) {
        return AccessControlEntitlementsModel.fromAnnotation(entitlementsOf(annotatedType));
    }

    @Nested
    class Building {

        @Test
        void fromAnnotation_severalClauses_modelCarriesEveryClauseAndMode() {
            AccessControlEntitlementsModel actualResult = modelOf(TwoClausesResource.class);

            assertThat(actualResult.getMode()).isEqualTo(AccessControlEntitlementsModel.Mode.ANY_OF);
            assertThat(actualResult.getGroups()).hasSize(2);
            assertThat(actualResult.getGroups().getFirst().getEntitlements()).isEqualTo(Set.of("ADMIN", "SUPPORT"));
            assertThat(actualResult.getGroups().getFirst().getMode())
                    .isEqualTo(EntitlementsGroupModel.Mode.ALL_OF);
        }

        @Test
        void fromAnnotation_containerModeOmitted_modeIsAllOf() {
            assertThat(modelOf(DefaultContainerModeResource.class).getMode())
                    .isEqualTo(AccessControlEntitlementsModel.Mode.ALL_OF);
        }

        @Test
        void fromAnnotation_groupModeOmitted_modeIsAnyOf() {
            assertThat(modelOf(DefaultContainerModeResource.class).getGroups().getFirst().getMode())
                    .isEqualTo(EntitlementsGroupModel.Mode.ANY_OF);
        }

        @Test
        void fromAnnotation_nothingDeclared_returnsNull() {
            assertThat(modelOf(NoRequirementResource.class)).isNull();
        }

        @Test
        void fromAnnotation_descriptionDeclared_modelCarriesIt() {
            assertThat(modelOf(DescribedResource.class).getDescription()).isEqualTo("internal partner tooling");
        }

        @Test
        void fromAnnotation_blankDescription_modelCarriesNull() {
            assertThat(modelOf(DefaultContainerModeResource.class).getDescription()).isNull();
        }

        @Test
        void fromAnnotation_duplicatedEntitlements_deduplicated() {
            assertThat(modelOf(DuplicatedEntitlementsResource.class).getGroups().getFirst().getEntitlements())
                    .isEqualTo(Set.of("ADMIN"));
        }

        @Test
        void fromAnnotation_clauseNamesNoEntitlement_throwsMisconfiguration() {
            assertThatThrownBy(() -> modelOf(EmptyClauseResource.class))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("at least one non-blank entitlement");
        }

        @Test
        void fromAnnotation_clauseNamesBlankEntitlement_throwsMisconfiguration() {
            assertThatThrownBy(() -> modelOf(BlankEntitlementResource.class))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("at least one non-blank entitlement");
        }

    }

    @Nested
    class PolicyDeclaration {

        @Test
        void fromAnnotation_policyDeclared_modelCarriesAnInstance() {
            AccessControlEntitlementsModel actualResult = modelOf(PolicyResource.class);

            assertThat(actualResult.getPolicy()).isInstanceOf(AdminPolicy.class);
            assertThat(actualResult.getGroups()).isEmpty();
        }

        @Test
        void fromAnnotation_policyDeclared_instantiatedOncePerModel() {
            // the instance is created while the model is built, not per evaluation
            AccessControlEntitlementsModel actualResult = modelOf(PolicyResource.class);

            assertThat(actualResult.getPolicy()).isSameAs(actualResult.getPolicy());
        }

        @Test
        void fromAnnotation_policyAndClausesBothDeclared_throwsMisconfiguration() {
            assertThatThrownBy(() -> modelOf(PolicyAndClausesResource.class))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("Declare one or the other");
        }

        @Test
        void fromAnnotation_policyWithoutNoArgConstructor_throwsMisconfiguration() {
            assertThatThrownBy(() -> modelOf(UninstantiablePolicyResource.class))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("no-argument constructor");
        }

    }

    @Nested
    class Matching {

        @Test
        void isSatisfiedBy_policyDeclared_delegatesToPolicy() {
            AccessControlEntitlementsModel sut = modelOf(PolicyResource.class);

            assertThat(sut.isSatisfiedBy(List.of("ADMIN"))).isTrue();
            assertThat(sut.isSatisfiedBy(List.of("PARTNER"))).isFalse();
        }

        @Test
        void isSatisfiedBy_anyOfClauses_onlyOneClauseNeedsToHold() {
            AccessControlEntitlementsModel sut = modelOf(TwoClausesResource.class);

            assertThat(sut.isSatisfiedBy(List.of("PARTNER", "PUBLIC"))).isTrue();
            assertThat(sut.isSatisfiedBy(List.of("ADMIN", "SUPPORT"))).isTrue();
            assertThat(sut.isSatisfiedBy(List.of("ADMIN", "PUBLIC"))).isFalse();
        }

        @Test
        void isSatisfiedBy_duplicatedHeldEntitlements_treatedAsOne() {
            AccessControlEntitlementsModel sut = modelOf(DefaultContainerModeResource.class);

            assertThat(sut.isSatisfiedBy(List.of("ADMIN", "ADMIN"))).isTrue();
        }

    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            mode = AccessControlEntitlements.Mode.ANY_OF,
            value = {
                    @EntitlementsGroup(value = {"ADMIN", "SUPPORT"}, mode = EntitlementsGroup.Mode.ALL_OF),
                    @EntitlementsGroup(value = {"PARTNER", "PUBLIC"}, mode = EntitlementsGroup.Mode.ALL_OF)
            }))
    private static class TwoClausesResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(@EntitlementsGroup("ADMIN")))
    private static class DefaultContainerModeResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            description = "internal partner tooling",
            value = @EntitlementsGroup("ADMIN")))
    private static class DescribedResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(@EntitlementsGroup({"ADMIN", "ADMIN"})))
    private static class DuplicatedEntitlementsResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(@EntitlementsGroup({})))
    private static class EmptyClauseResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(@EntitlementsGroup({"ADMIN", "   "})))
    private static class BlankEntitlementResource {
    }

    @AccessControl
    private static class NoRequirementResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(policy = AdminPolicy.class))
    private static class PolicyResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(
            policy = AdminPolicy.class,
            value = @EntitlementsGroup("ADMIN")))
    private static class PolicyAndClausesResource {
    }

    @AccessControl(entitlements = @AccessControlEntitlements(policy = UninstantiablePolicy.class))
    private static class UninstantiablePolicyResource {
    }

    public static class AdminPolicy implements EntitlementsPolicy {
        @Override
        public boolean isSatisfiedBy(List<String> entitlements) {
            return entitlements.contains("ADMIN");
        }
    }

    public static class UninstantiablePolicy implements EntitlementsPolicy {

        public UninstantiablePolicy(String required) {
            // no no-arg constructor on purpose
        }

        @Override
        public boolean isSatisfiedBy(List<String> entitlements) {
            return true;
        }
    }

}
