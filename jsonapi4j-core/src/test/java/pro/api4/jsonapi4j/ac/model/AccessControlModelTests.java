package pro.api4.jsonapi4j.ac.model;

import org.junit.jupiter.api.Test;
import pro.api4.jsonapi4j.ac.annotation.Authenticated;
import pro.api4.jsonapi4j.ac.ownership.ResourceIdFromUrlPathExtractor;
import pro.api4.jsonapi4j.ac.tier.TierAdmin;
import pro.api4.jsonapi4j.ac.tier.TierRootAdmin;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

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
                .requiredAccessTier(AccessControlAccessTierModel.builder().requiredAccessTier(TierAdmin.ADMIN_ACCESS_TIER).build())
                .requiredScopes(AccessControlScopesModel.builder().requiredScopesExpression("bla bla").build())
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
        assertThat(result.getRequiredAccessTier())
                .isNotNull()
                .extracting(AccessControlAccessTierModel::getRequiredAccessTier)
                .isEqualTo(TierAdmin.ADMIN_ACCESS_TIER);
        assertThat(result.getRequiredScopes())
                .isNotNull()
                .extracting(AccessControlScopesModel::getRequiredScopesExpression)
                .isEqualTo("bla bla");
        assertThat(result.getRequiredScopes().getRequiredScopes()).isNull();
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
                .requiredAccessTier(AccessControlAccessTierModel.builder().requiredAccessTier(TierAdmin.ADMIN_ACCESS_TIER).build())
                .requiredScopes(AccessControlScopesModel.builder().requiredScopesExpression("bla bla").build())
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
        assertThat(result.getRequiredAccessTier())
                .isNotNull()
                .extracting(AccessControlAccessTierModel::getRequiredAccessTier)
                .isEqualTo(TierAdmin.ADMIN_ACCESS_TIER);
        assertThat(result.getRequiredScopes())
                .isNotNull()
                .extracting(AccessControlScopesModel::getRequiredScopesExpression)
                .isEqualTo("bla bla");
        assertThat(result.getRequiredScopes().getRequiredScopes()).isNull();
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
                .requiredAccessTier(AccessControlAccessTierModel.builder().requiredAccessTier(TierAdmin.ADMIN_ACCESS_TIER).build())
                .requiredScopes(AccessControlScopesModel.builder().requiredScopesExpression("bla bla").build())
                .requiredOwnership(AccessControlOwnershipModel.builder().ownerIdExtractor(ResourceIdFromUrlPathExtractor.class).build())
                .build();

        AccessControlModel higherPrecedence = AccessControlModel.builder()
                .authenticated(AccessControlAuthenticatedModel.builder().authenticated(Authenticated.ANONYMOUS).build())
                .requiredAccessTier(AccessControlAccessTierModel.builder().requiredAccessTier(TierRootAdmin.ROOT_ADMIN_ACCESS_TIER).build())
                .requiredScopes(AccessControlScopesModel.builder().requiredScopes(Set.of("bla", "bla2")).build())
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
        assertThat(result.getRequiredAccessTier())
                .isNotNull()
                .extracting(AccessControlAccessTierModel::getRequiredAccessTier)
                .isEqualTo(TierRootAdmin.ROOT_ADMIN_ACCESS_TIER);
        assertThat(result.getRequiredScopes()).isNotNull();
        assertThat(result.getRequiredScopes().getRequiredScopes()).isEqualTo(Set.of("bla", "bla2"));
        assertThat(result.getRequiredScopes().getRequiredScopesExpression()).isNull();
        assertThat(result.getRequiredOwnership())
                .isNotNull()
                .extracting(AccessControlOwnershipModel::getOwnerIdFieldPath)
                .isEqualTo("id");
        assertThat(result.getRequiredOwnership().getOwnerIdExtractor()).isNull();
    }

}
