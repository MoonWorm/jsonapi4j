package pro.api4.jsonapi4j.plugin.ac.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;
import pro.api4.jsonapi4j.plugin.ac.exception.AccessControlMisconfigurationException;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AccessControlScopesModelTests {

    private static AccessControlScopesModel modelOf(Class<?> annotatedType) {
        return AccessControlScopesModel.fromAnnotation(
                annotatedType.getAnnotation(AccessControl.class).scopes());
    }

    @Nested
    class Building {

        @Test
        void fromAnnotation_severalClauses_modelCarriesEveryClauseAndMode() {
            AccessControlScopesModel actualResult = modelOf(TwoClausesResource.class);

            assertThat(actualResult.getMode()).isEqualTo(AccessControlScopesModel.Mode.ANY_OF);
            assertThat(actualResult.getGroups()).hasSize(2);
            assertThat(actualResult.getGroups().getFirst().getScopes())
                    .isEqualTo(Set.of("users.read", "profiles.read"));
        }

        @Test
        void fromAnnotation_containerModeOmitted_modeIsAllOf() {
            assertThat(modelOf(SingleClauseResource.class).getMode())
                    .isEqualTo(AccessControlScopesModel.Mode.ALL_OF);
        }

        @Test
        void fromAnnotation_groupModeOmitted_modeIsAllOf() {
            // scopes normally ask for every scope they list, unlike entitlements which default to ANY_OF
            assertThat(modelOf(SingleClauseResource.class).getGroups().getFirst().getMode())
                    .isEqualTo(ScopesGroupModel.Mode.ALL_OF);
        }

        @Test
        void fromAnnotation_nothingDeclared_returnsNull() {
            assertThat(modelOf(NoRequirementResource.class)).isNull();
        }

        @Test
        void fromAnnotation_descriptionDeclared_modelCarriesIt() {
            assertThat(modelOf(DescribedResource.class).getDescription()).isEqualTo("sensitive profile access");
        }

        @Test
        void fromAnnotation_clauseNamesNoScope_throwsMisconfiguration() {
            assertThatThrownBy(() -> modelOf(EmptyClauseResource.class))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("at least one non-blank scope");
        }

        @Test
        void fromAnnotation_clauseNamesBlankScope_throwsMisconfiguration() {
            assertThatThrownBy(() -> modelOf(BlankScopeResource.class))
                    .isInstanceOf(AccessControlMisconfigurationException.class)
                    .hasMessageContaining("at least one non-blank scope");
        }

        @Test
        void fromAnnotation_builtModel_clauseNamesRejectMutation() {
            AccessControlScopesModel actualResult = modelOf(SingleClauseResource.class);

            assertThatThrownBy(() -> actualResult.getGroups().getFirst().getScopes().clear())
                    .isInstanceOf(UnsupportedOperationException.class);
        }

    }

    @Nested
    class Matching {

        @Test
        void isSatisfiedBy_allOfClause_needsEveryScope() {
            AccessControlScopesModel sut = modelOf(SingleClauseResource.class);

            assertThat(sut.isSatisfiedBy(Set.of("users.read", "users.write"))).isTrue();
            assertThat(sut.isSatisfiedBy(Set.of("users.read"))).isFalse();
        }

        @Test
        void isSatisfiedBy_allOfClauseAndScopesBeyondRequired_stillSatisfied() {
            AccessControlScopesModel sut = modelOf(SingleClauseResource.class);

            assertThat(sut.isSatisfiedBy(Set.of("users.read", "users.write", "admin.full"))).isTrue();
        }

        @Test
        void isSatisfiedBy_anyOfClauses_onlyOneClauseNeedsToHold() {
            AccessControlScopesModel sut = modelOf(TwoClausesResource.class);

            assertThat(sut.isSatisfiedBy(Set.of("admin.full"))).isTrue();
            assertThat(sut.isSatisfiedBy(Set.of("users.read", "profiles.read"))).isTrue();
            assertThat(sut.isSatisfiedBy(Set.of("users.read"))).isFalse();
        }

        @Test
        void isSatisfiedBy_noneOfClause_deniesWhenTheScopeIsGranted() {
            AccessControlScopesModel sut = modelOf(NoneOfResource.class);

            assertThat(sut.isSatisfiedBy(Set.of("users.read"))).isTrue();
            assertThat(sut.isSatisfiedBy(Set.of("users.read", "readonly"))).isFalse();
        }

        @Test
        void isSatisfiedBy_noScopesGranted_deniesUnlessTheRuleIsNegative() {
            assertThat(modelOf(SingleClauseResource.class).isSatisfiedBy(Set.of())).isFalse();
            assertThat(modelOf(NoneOfResource.class).isSatisfiedBy(Set.of())).isTrue();
        }

    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup({"users.read", "users.write"})))
    private static class SingleClauseResource {
    }

    @AccessControl(scopes = @AccessControlScopes(
            mode = AccessControlScopes.Mode.ANY_OF,
            value = {
                    @ScopesGroup({"users.read", "profiles.read"}),
                    @ScopesGroup("admin.full")
            }))
    private static class TwoClausesResource {
    }

    @AccessControl(scopes = @AccessControlScopes(
            @ScopesGroup(value = "readonly", mode = ScopesGroup.Mode.NONE_OF)))
    private static class NoneOfResource {
    }

    @AccessControl(scopes = @AccessControlScopes(
            description = "sensitive profile access",
            value = @ScopesGroup("profiles.sensitive.read")))
    private static class DescribedResource {
    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup({})))
    private static class EmptyClauseResource {
    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup({"users.read", "   "})))
    private static class BlankScopeResource {
    }

    @AccessControl
    private static class NoRequirementResource {
    }

}
