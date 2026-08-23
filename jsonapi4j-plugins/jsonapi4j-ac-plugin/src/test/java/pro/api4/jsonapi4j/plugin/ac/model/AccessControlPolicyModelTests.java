package pro.api4.jsonapi4j.plugin.ac.model;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlPolicy;
import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;
import pro.api4.jsonapi4j.plugin.ac.policy.AccessPolicy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessControlPolicyModelTests {

    private static AccessControlPolicyModel modelOf(Class<?> annotatedType) {
        return AccessControlPolicyModel.fromAnnotation(
                annotatedType.getAnnotation(AccessControl.class).policy());
    }

    @Test
    void fromAnnotation_policyDeclared_modelCarriesAnInstance() {
        assertThat(modelOf(PolicyResource.class).getPolicy()).isInstanceOf(AllowAllPolicy.class);
    }

    @Test
    void fromAnnotation_policyDeclared_instantiatedOncePerModel() {
        // the instance is created while the model is built, not per evaluation
        AccessControlPolicyModel actualResult = modelOf(PolicyResource.class);

        assertThat(actualResult.getPolicy()).isSameAs(actualResult.getPolicy());
    }

    @Test
    void fromAnnotation_noPolicyDeclared_returnsNull() {
        assertThat(modelOf(NoPolicyResource.class)).isNull();
    }

    @Test
    void fromAnnotation_descriptionDeclared_modelCarriesIt() {
        assertThat(modelOf(DescribedPolicyResource.class).getDescription()).isEqualTo("only on Tuesdays");
    }

    @Test
    void fromAnnotation_blankDescription_modelCarriesNull() {
        assertThat(modelOf(PolicyResource.class).getDescription()).isNull();
    }

    @Test
    void fromAnnotation_policyWithoutNoArgConstructor_throwsMisconfiguration() {
        assertThatThrownBy(() -> modelOf(UninstantiablePolicyResource.class))
                .isInstanceOf(AccessControlMisconfigurationException.class)
                .hasMessageContaining("no-argument constructor");
    }

    @Test
    void isSatisfiedBy_delegatesToTheDeclaredPolicy() {
        assertThat(modelOf(PolicyResource.class).isSatisfiedBy(null)).isTrue();
        assertThat(modelOf(DenyAllPolicyResource.class).isSatisfiedBy(null)).isFalse();
    }

    @AccessControl(policy = @AccessControlPolicy(AllowAllPolicy.class))
    private static class PolicyResource {
    }

    @AccessControl(policy = @AccessControlPolicy(DenyAllPolicy.class))
    private static class DenyAllPolicyResource {
    }

    @AccessControl(policy = @AccessControlPolicy(
            value = AllowAllPolicy.class,
            description = "only on Tuesdays"))
    private static class DescribedPolicyResource {
    }

    @AccessControl(policy = @AccessControlPolicy(UninstantiablePolicy.class))
    private static class UninstantiablePolicyResource {
    }

    @AccessControl
    private static class NoPolicyResource {
    }

    public static class AllowAllPolicy implements AccessPolicy {
        @Override
        public boolean isSatisfiedBy(AccessControlContext context) {
            return true;
        }
    }

    public static class DenyAllPolicy implements AccessPolicy {
        @Override
        public boolean isSatisfiedBy(AccessControlContext context) {
            return false;
        }
    }

    public static class UninstantiablePolicy implements AccessPolicy {

        public UninstantiablePolicy(String required) {
            // no no-arg constructor on purpose
        }

        @Override
        public boolean isSatisfiedBy(AccessControlContext context) {
            return true;
        }
    }

}
