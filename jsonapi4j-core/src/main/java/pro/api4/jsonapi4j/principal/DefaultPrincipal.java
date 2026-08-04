
package pro.api4.jsonapi4j.principal;

import pro.api4.jsonapi4j.principal.tier.AccessTier;

import java.util.Map;
import java.util.Set;

public record DefaultPrincipal(AccessTier authenticatedClientAccessTier,
                               Set<String> authenticatedClientScopes,
                               String authenticatedUserId,
                               Map<String, Object> attributes) implements Principal {

    /**
     * Creates a principal without custom attributes, as carried by authentication flows that expose no
     * additional claims (e.g. header-based or server-to-server flows).
     *
     * @param authenticatedClientAccessTier the client's access tier
     * @param authenticatedClientScopes     the granted scopes
     * @param authenticatedUserId           the authenticated user id
     */
    public DefaultPrincipal(AccessTier authenticatedClientAccessTier,
                            Set<String> authenticatedClientScopes,
                            String authenticatedUserId) {
        this(authenticatedClientAccessTier, authenticatedClientScopes, authenticatedUserId, Map.of());
    }

}
