package pro.api4.jsonapi4j.principal;

import pro.api4.jsonapi4j.principal.tier.AccessTier;

import java.util.Optional;
import java.util.Set;

/**
 * Thread-local holder for the {@link Principal} of the currently authenticated request.
 * <p>
 * The servlet layer sets the principal on the incoming thread at the start of each request
 * and clears it when the request completes. Plugins and application code can read the
 * principal at any point during request processing using the static accessor methods.
 * <p>
 * Not thread-safe for cross-thread use — all accesses must occur on the same request thread.
 */
public class AuthenticatedPrincipalContextHolder {

    private static final ThreadLocal<Principal> PRINCIPAL = new ThreadLocal<>();

    /**
     * Stores the given {@link Principal} in the current thread's context.
     * Called by the principal-resolving servlet filter at the start of each request.
     *
     * @param principal the authenticated principal, or {@code null} to clear the context
     */
    public static void setAuthenticatedPrincipalContext(Principal principal) {
        PRINCIPAL.set(principal);
    }

    /**
     * Returns a defensive copy of the current thread's {@link Principal}, or {@code null}
     * if no principal is set. The copy shares the same field values but is a separate object,
     * safe to pass to asynchronous contexts.
     *
     * @return a copy of the current principal, or {@code null}
     */
    public static Principal copy() {
        Principal principal = PRINCIPAL.get();
        if (principal == null) {
            return null;
        }
        return new Principal() {
            @Override
            public String authenticatedUserId() {
                return principal.authenticatedUserId();
            }

            @Override
            public AccessTier authenticatedClientAccessTier() {
                return principal.authenticatedClientAccessTier();
            }

            @Override
            public Set<String> authenticatedClientScopes() {
                return principal.authenticatedClientScopes();
            }
        };
    }

    /**
     * Returns the {@link AccessTier} of the current principal, or an empty {@link Optional}
     * if no principal is set.
     *
     * @return optional access tier
     */
    public static Optional<AccessTier> getAccessTier() {
        return Optional.ofNullable(PRINCIPAL.get()).map(Principal::authenticatedClientAccessTier);
    }

    /**
     * Returns the scopes of the current principal, or an empty {@link Optional}
     * if no principal is set.
     *
     * @return optional set of scope strings
     */
    public static Optional<Set<String>> getScopes() {
        return Optional.ofNullable(PRINCIPAL.get()).map(Principal::authenticatedClientScopes);
    }

    /**
     * Returns the authenticated user id of the current principal, or an empty {@link Optional}
     * if no principal is set.
     *
     * @return optional user id
     */
    public static Optional<String> getAuthenticatedUserId() {
        return Optional.ofNullable(PRINCIPAL.get()).map(Principal::authenticatedUserId);
    }

    /**
     * Returns the current principal as an {@link Optional}, or an empty {@link Optional}
     * if no principal is set.
     *
     * @return optional principal
     */
    public static Optional<Principal> getPrincipal() {
        return Optional.ofNullable(PRINCIPAL.get());
    }

}
