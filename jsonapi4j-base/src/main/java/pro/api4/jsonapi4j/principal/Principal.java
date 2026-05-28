package pro.api4.jsonapi4j.principal;

import pro.api4.jsonapi4j.principal.tier.AccessTier;

import java.util.Set;

/**
 * Represents the authenticated caller of a JSON:API request.
 * <p>
 * Implementations are set on the current thread by the servlet filter layer
 * (via {@link AuthenticatedPrincipalContextHolder#setAuthenticatedPrincipalContext(Principal)})
 * and consumed by plugins such as the Access Control plugin to make authorization decisions.
 * <p>
 * Applications must provide their own implementation of this interface, resolving the principal
 * from the incoming HTTP request (e.g. from a JWT token, session, or API key).
 */
public interface Principal {

    /**
     * Returns the unique identifier of the authenticated user (e.g. a UUID or username).
     *
     * @return user id, never {@code null} for authenticated requests
     */
    String authenticatedUserId();

    /**
     * Returns the access tier assigned to the authenticated client.
     * Access tiers are used by the Access Control plugin to enforce coarse-grained
     * authorization rules (e.g. {@code PUBLIC}, {@code INTERNAL}, {@code ADMIN}).
     *
     * @return the client's {@link AccessTier}, never {@code null} for authenticated requests
     */
    AccessTier authenticatedClientAccessTier();

    /**
     * Returns the set of OAuth2/custom scopes granted to the authenticated client.
     * Scopes are used by the Access Control plugin to enforce fine-grained authorization rules.
     *
     * @return set of scope strings, never {@code null} (may be empty)
     */
    Set<String> authenticatedClientScopes();

}
