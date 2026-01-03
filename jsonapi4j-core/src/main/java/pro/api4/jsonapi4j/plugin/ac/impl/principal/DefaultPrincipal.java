
package pro.api4.jsonapi4j.plugin.ac.impl.principal;

import pro.api4.jsonapi4j.plugin.ac.impl.tier.AccessTier;

import java.util.Set;

public record DefaultPrincipal(AccessTier authenticatedClientAccessTier,
                               Set<String> authenticatedClientScopes,
                               String authenticatedUserId) implements Principal {

}
