package io.jsonapi4j.plugin.ac.model;

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
            .requireAuthenticatedUser(AccessControlAuthenticatedModel.DEFAULT)
            .requiredAccessTier(AccessControlAccessTierModel.DEFAULT)
            .requiredScopes(AccessControlScopesModel.DEFAULT)
            .requiredOwnership(AccessControlOwnershipModel.DEFAULT)
            .build();

    private AccessControlAuthenticatedModel requireAuthenticatedUser;
    private AccessControlAccessTierModel requiredAccessTier;
    private AccessControlScopesModel requiredScopes;
    private AccessControlOwnershipModel requiredOwnership;

}
