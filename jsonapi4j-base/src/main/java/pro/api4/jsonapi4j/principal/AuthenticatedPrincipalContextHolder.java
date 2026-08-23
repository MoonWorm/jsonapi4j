package pro.api4.jsonapi4j.principal;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        if (principal == null) {
            clear();
            return;
        }
        PRINCIPAL.set(principal);
    }

    /**
     * Removes the current thread's {@link Principal}.
     * <p>
     * Called by the principal-resolving servlet filter once the request completes. Request threads are
     * pooled and reused, so a principal left behind would be visible to whatever the container runs on that
     * thread next — including work that never passes through the filter. Clearing removes the thread-local
     * entry outright rather than setting it to {@code null}, so nothing is retained between requests.
     */
    public static void clear() {
        PRINCIPAL.remove();
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
            public List<String> authenticatedClientEntitlements() {
                return principal.authenticatedClientEntitlements();
            }

            @Override
            public Set<String> authenticatedClientScopes() {
                return principal.authenticatedClientScopes();
            }

            @Override
            public Map<String, Object> attributes() {
                return principal.attributes();
            }
        };
    }

    /**
     * Returns the entitlements of the current principal, or an empty list if no principal is set or the
     * principal carries none. Unlike the other accessors here, this never returns {@code null} and is not
     * wrapped in an {@link Optional} — an absent principal and one without entitlements are treated alike,
     * since both fail every entitlement requirement.
     *
     * @return the current principal's entitlements, never {@code null} (may be empty)
     */
    public static List<String> getEntitlements() {
        return Optional.ofNullable(PRINCIPAL.get())
                .map(Principal::authenticatedClientEntitlements)
                .orElse(Collections.emptyList());
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
     * Returns the authenticated user-specific claims (e.g. email, expiration date, etc), or an empty {@link Optional}
     * if no principal is set.
     *
     * @return optional user-specific claims
     */
    public static Optional<Map<String, Object>> getAttributes() {
        return Optional.ofNullable(PRINCIPAL.get()).map(Principal::attributes);
    }

    /**
     * Returns the current principal as an {@link Optional}, or an empty {@link Optional}
     * if no principal is set or for server-to-server auth flows.
     *
     * @return optional principal
     */
    public static Optional<Principal> getPrincipal() {
        return Optional.ofNullable(PRINCIPAL.get());
    }

}
