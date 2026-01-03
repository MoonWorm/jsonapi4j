package pro.api4.jsonapi4j.plugin.ac.impl.principal;

import pro.api4.jsonapi4j.plugin.ac.impl.tier.AccessTier;

import java.util.Optional;
import java.util.Set;

public class AuthenticatedPrincipalContextHolder {

    private static final ThreadLocal<Principal> PRINCIPAL = new ThreadLocal<>();

    public static void setAuthenticatedPrincipalContext(AccessTier accessTier,
                                                        Set<String> scopes,
                                                        String userId) {
        PRINCIPAL.set(new DefaultPrincipal(accessTier, scopes, userId));
    }

    public static void setAuthenticatedPrincipalContext(Principal principal) {
        PRINCIPAL.set(principal);
    }

    public static Principal copy() {
        Principal principal = PRINCIPAL.get();
        if (principal == null) {
            return null;
        }
        return new DefaultPrincipal(
                principal.authenticatedClientAccessTier(),
                principal.authenticatedClientScopes(),
                principal.authenticatedUserId()
        );
    }

    public static Optional<AccessTier> getAccessTier() {
        return Optional.ofNullable(PRINCIPAL.get()).map(Principal::authenticatedClientAccessTier);
    }

    public static Optional<Set<String>> getScopes() {
        return Optional.ofNullable(PRINCIPAL.get()).map(Principal::authenticatedClientScopes);
    }

    public static Optional<String> getAuthenticatedUserId() {
        return Optional.ofNullable(PRINCIPAL.get()).map(Principal::authenticatedUserId);
    }

    public static Optional<Principal> getPrincipal() {
        return Optional.ofNullable(PRINCIPAL.get());
    }

}
