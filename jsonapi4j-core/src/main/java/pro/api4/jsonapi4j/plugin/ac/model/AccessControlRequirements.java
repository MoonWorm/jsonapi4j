package pro.api4.jsonapi4j.plugin.ac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Builder
public class AccessControlRequirements {

    public static AccessControlRequirements DEFAULT = AccessControlRequirements.builder()
            .authenticated(AccessControlAuthenticatedModel.DEFAULT)
            .requiredAccessTier(AccessControlAccessTierModel.DEFAULT)
            .requiredScopes(AccessControlScopesModel.DEFAULT)
            .requiredOwnership(AccessControlOwnershipModel.DEFAULT)
            .build();

    private AccessControlAuthenticatedModel authenticated;
    private AccessControlAccessTierModel requiredAccessTier;
    private AccessControlScopesModel requiredScopes;
    private AccessControlOwnershipModel requiredOwnership;

    public static AccessControlRequirements merge(AccessControlRequirements master,
                                                  AccessControlRequirements other) {
        if (master == null && other == null) {
            return null;
        }
        AccessControlRequirements result = new AccessControlRequirements();
        if (other != null && other.getAuthenticated() != null) {
            result.setAuthenticated(
                    new AccessControlAuthenticatedModel(other.getAuthenticated().getAuthenticated())
            );
        } else if (master != null && master.getAuthenticated() != null) {
            result.setAuthenticated(
                    new AccessControlAuthenticatedModel(master.getAuthenticated().getAuthenticated())
            );
        }
        if (other != null && other.getRequiredAccessTier() != null) {
            result.setRequiredAccessTier(
                    new AccessControlAccessTierModel(other.getRequiredAccessTier().getRequiredAccessTier())
            );
        } else if (master != null && master.getRequiredAccessTier() != null) {
            result.setRequiredAccessTier(
                    new AccessControlAccessTierModel(master.getRequiredAccessTier().getRequiredAccessTier())
            );
        }
        if (other != null && other.getRequiredScopes() != null) {
            result.setRequiredScopes(
                    new AccessControlScopesModel(
                            other.getRequiredScopes().getRequiredScopes(),
                            other.getRequiredScopes().getRequiredScopesExpression()
                    )
            );
        } else if (master != null && master.getRequiredScopes() != null) {
            result.setRequiredScopes(
                    new AccessControlScopesModel(
                            master.getRequiredScopes().getRequiredScopes(),
                            master.getRequiredScopes().getRequiredScopesExpression()
                    )
            );
        }
        if (other != null && other.getRequiredOwnership() != null) {
            result.setRequiredOwnership(
                    new AccessControlOwnershipModel(
                            other.getRequiredOwnership().getOwnerIdFieldPath(),
                            other.getRequiredOwnership().getOwnerIdExtractor()
                    )
            );
        } else if (master != null && master.getRequiredOwnership() != null) {
            result.setRequiredOwnership(
                    new AccessControlOwnershipModel(
                            master.getRequiredOwnership().getOwnerIdFieldPath(),
                            master.getRequiredOwnership().getOwnerIdExtractor()
                    )
            );
        }
        return result;
    }

}
