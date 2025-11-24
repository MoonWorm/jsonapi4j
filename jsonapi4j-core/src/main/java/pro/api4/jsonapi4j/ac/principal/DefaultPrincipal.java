
package pro.api4.jsonapi4j.ac.principal;

import pro.api4.jsonapi4j.ac.tier.AccessTier;

import java.util.Set;

public record DefaultPrincipal(AccessTier authenticatedClientAccessTier,
                               Set<String> authenticatedClientScopes,
                               String authenticatedUserId) implements Principal {

}
