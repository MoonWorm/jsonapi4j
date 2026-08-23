package pro.api4.jsonapi4j.principal;

import java.util.List;
import java.util.Map;
import java.util.Set;

public record DefaultPrincipal(List<String> authenticatedClientEntitlements,
                               Set<String> authenticatedClientScopes,
                               String authenticatedUserId,
                               Map<String, Object> attributes) implements Principal {

    /**
     * Creates a principal without custom attributes, as carried by authentication flows that expose no
     * additional claims (e.g. header-based or server-to-server flows).
     *
     * @param authenticatedClientEntitlements the client's entitlements
     * @param authenticatedClientScopes     the granted scopes
     * @param authenticatedUserId           the authenticated user id
     */
    public DefaultPrincipal(List<String> authenticatedClientEntitlements,
                            Set<String> authenticatedClientScopes,
                            String authenticatedUserId) {
        this(authenticatedClientEntitlements, authenticatedClientScopes, authenticatedUserId, Map.of());
    }

}
