package pro.api4.jsonapi4j.plugin.ac.model;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControl;
import pro.api4.jsonapi4j.plugin.ac.annotation.AccessControlScopes;
import pro.api4.jsonapi4j.plugin.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.plugin.ac.annotation.ScopesGroup;
import pro.api4.jsonapi4j.plugin.ac.ownership.ResourceIdFromUrlPathExtractor;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.ADMIN;
import static pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements.ROOT_ADMIN;

public class AccessControlModelTests {

    @Test
    public void merge_checkBothAreNull_checkResult() {
        // given - when - then
        assertThat(AccessControlModel.merge(null, null)).isNull();
    }

    @Test
    public void merge_checkFirstIsNotNullAndSecondIsNull_checkResult() {
        // given - when
        AccessControlModel lowerPrecedence = AccessControlModel.builder().build();
        AccessControlModel result = AccessControlModel.merge(
                lowerPrecedence,
                null
        );

        // then
        assertThat(result).isNotNull().isEqualTo(lowerPrecedence);
    }

    @Test
    public void merge_checkFirstIsNullAndSecondIsNotNull_checkResult() {
        // given - when
        AccessControlModel higherPrecedence = AccessControlModel.builder().build();
        AccessControlModel result = AccessControlModel.merge(
                null,
                higherPrecedence
        );

        // then
        assertThat(result).isNotNull().isEqualTo(higherPrecedence);
    }

    @Test
    public void merge_checkHigherPrecedenceModelHasDataAndOtherHasDefault_checkResult() {
        // given
        AccessControlModel lowerPrecedence = AccessControlModel.builder().build();

        AccessControlModel higherPrecedence = AccessControlModel.builder()
                .authenticated(AccessControlAuthenticatedModel.builder().authenticated(Authenticated.AUTHENTICATED).build())
                .requiredEntitlements(AccessControlEntitlementsModel.builder()
                        .groups(List.of(EntitlementsGroupModel.builder().entitlements(Set.of(ADMIN)).build()))
                        .build())
                .requiredScopes(AccessControlScopesModel.builder()
                        .groups(List.of(ScopesGroupModel.builder().scopes(Set.of("bla")).build()))
                        .build())
                .requiredOwnership(AccessControlOwnershipModel.builder().ownerIdFieldPath("id").build())
                .build();

        // when
        AccessControlModel result = AccessControlModel.merge(lowerPrecedence, higherPrecedence);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAuthenticated())
                .isNotNull()
                .extracting(AccessControlAuthenticatedModel::getAuthenticated)
                .isEqualTo(Authenticated.AUTHENTICATED);
        assertThat(result.getRequiredEntitlements()).isNotNull();
        assertThat(result.getRequiredEntitlements().getGroups().getFirst().getEntitlements())
                .isEqualTo(Set.of(ADMIN));
        assertThat(result.getRequiredScopes()).isNotNull();
        assertThat(result.getRequiredScopes().getGroups().getFirst().getScopes()).isEqualTo(Set.of("bla"));
        assertThat(result.getRequiredOwnership())
                .isNotNull()
                .extracting(AccessControlOwnershipModel::getOwnerIdFieldPath)
                .isEqualTo("id");
        assertThat(result.getRequiredOwnership().getOwnerIdExtractor()).isNull();
    }

    @Test
    public void merge_checkHigherPrecedenceModelHasDefaultModelAndOtherHasCustomData_checkResult() {
        // given
        AccessControlModel lowerPrecedence = AccessControlModel.builder()
                .authenticated(AccessControlAuthenticatedModel.builder().authenticated(Authenticated.AUTHENTICATED).build())
                .requiredEntitlements(AccessControlEntitlementsModel.builder()
                        .groups(List.of(EntitlementsGroupModel.builder().entitlements(Set.of(ADMIN)).build()))
                        .build())
                .requiredScopes(AccessControlScopesModel.builder()
                        .groups(List.of(ScopesGroupModel.builder().scopes(Set.of("bla")).build()))
                        .build())
                .requiredOwnership(AccessControlOwnershipModel.builder().ownerIdFieldPath("id").build())
                .build();

        AccessControlModel higherPrecedence = AccessControlModel.builder().build();

        // when
        AccessControlModel result = AccessControlModel.merge(lowerPrecedence, higherPrecedence);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAuthenticated())
                .isNotNull()
                .extracting(AccessControlAuthenticatedModel::getAuthenticated)
                .isEqualTo(Authenticated.AUTHENTICATED);
        assertThat(result.getRequiredEntitlements()).isNotNull();
        assertThat(result.getRequiredEntitlements().getGroups().getFirst().getEntitlements())
                .isEqualTo(Set.of(ADMIN));
        assertThat(result.getRequiredScopes()).isNotNull();
        assertThat(result.getRequiredScopes().getGroups().getFirst().getScopes()).isEqualTo(Set.of("bla"));
        assertThat(result.getRequiredOwnership())
                .isNotNull()
                .extracting(AccessControlOwnershipModel::getOwnerIdFieldPath)
                .isEqualTo("id");
        assertThat(result.getRequiredOwnership().getOwnerIdExtractor()).isNull();
    }

    @Test
    public void merge_checkBothHaveCustomData_checkMixedResult() {
        // given
        AccessControlModel lowerPrecedence = AccessControlModel.builder()
                .authenticated(AccessControlAuthenticatedModel.builder().authenticated(Authenticated.AUTHENTICATED).build())
                .requiredEntitlements(AccessControlEntitlementsModel.builder()
                        .groups(List.of(EntitlementsGroupModel.builder().entitlements(Set.of(ADMIN)).build()))
                        .build())
                .requiredScopes(AccessControlScopesModel.builder()
                        .groups(List.of(ScopesGroupModel.builder().scopes(Set.of("bla")).build()))
                        .build())
                .requiredOwnership(AccessControlOwnershipModel.builder().ownerIdExtractor(ResourceIdFromUrlPathExtractor.class).build())
                .build();

        AccessControlModel higherPrecedence = AccessControlModel.builder()
                .authenticated(AccessControlAuthenticatedModel.builder().authenticated(Authenticated.ANONYMOUS).build())
                .requiredEntitlements(AccessControlEntitlementsModel.builder()
                        .groups(List.of(EntitlementsGroupModel.builder().entitlements(Set.of(ROOT_ADMIN)).build()))
                        .build())
                .requiredScopes(AccessControlScopesModel.builder()
                        .groups(List.of(ScopesGroupModel.builder().scopes(Set.of("bla", "bla2")).build()))
                        .build())
                .requiredOwnership(AccessControlOwnershipModel.builder().ownerIdFieldPath("id").build())
                .build();

        // when
        AccessControlModel result = AccessControlModel.merge(lowerPrecedence, higherPrecedence);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getAuthenticated())
                .isNotNull()
                .extracting(AccessControlAuthenticatedModel::getAuthenticated)
                .isEqualTo(Authenticated.ANONYMOUS);
        assertThat(result.getRequiredEntitlements()).isNotNull();
        assertThat(result.getRequiredEntitlements().getGroups().getFirst().getEntitlements())
                .isEqualTo(Set.of(ROOT_ADMIN));
        assertThat(result.getRequiredScopes()).isNotNull();
        assertThat(result.getRequiredScopes().getGroups().getFirst().getScopes()).isEqualTo(Set.of("bla", "bla2"));
        assertThat(result.getRequiredOwnership())
                .isNotNull()
                .extracting(AccessControlOwnershipModel::getOwnerIdFieldPath)
                .isEqualTo("id");
        assertThat(result.getRequiredOwnership().getOwnerIdExtractor()).isNull();
    }


    @Nested
    class Caching {

        @Test
        void fromClassAnnotation_sameClassTwice_returnsTheSameInstance() {
            AccessControlModel first = AccessControlModel.fromClassAnnotation(AnnotatedResource.class);

            assertThat(AccessControlModel.fromClassAnnotation(AnnotatedResource.class)).isSameAs(first);
        }

        @Test
        void fromClassAnnotation_unannotatedClass_returnsNullEveryTime() {
            assertThat(AccessControlModel.fromClassAnnotation(UnannotatedResource.class)).isNull();
            assertThat(AccessControlModel.fromClassAnnotation(UnannotatedResource.class)).isNull();
        }

        @Test
        void fromFieldsAnnotations_sameClassTwice_returnsTheSameInstance() {
            Map<String, AccessControlModel> first = AccessControlModel.fromFieldsAnnotations(AnnotatedResource.class);

            assertThat(AccessControlModel.fromFieldsAnnotations(AnnotatedResource.class)).isSameAs(first);
        }

        @Test
        void fromFieldsAnnotations_cachedResult_rejectsMutation() {
            Map<String, AccessControlModel> actualResult
                    = AccessControlModel.fromFieldsAnnotations(AnnotatedResource.class);

            assertThatThrownBy(actualResult::clear).isInstanceOf(UnsupportedOperationException.class);
        }

        @Test
        void fromClassAnnotation_differentClasses_returnDifferentInstances() {
            assertThat(AccessControlModel.fromClassAnnotation(AnnotatedResource.class))
                    .isNotSameAs(AccessControlModel.fromClassAnnotation(OtherAnnotatedResource.class));
        }

    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("things.read")))
    private static class AnnotatedResource {

        @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("things.secret.read")))
        private String secret;

    }

    @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("others.read")))
    private static class OtherAnnotatedResource {
    }

    private static class UnannotatedResource {
    }
}
