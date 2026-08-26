package pro.api4.jsonapi4j.principal;

import java.util.List;
import java.util.Map;
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
     * Returns the entitlements held by the authenticated client — opaque labels naming what the client may
     * reach (e.g. {@code PARTNER}, {@code ADMIN}, {@code SUPPORT}), used by the Access Control plugin to
     * enforce coarse-grained authorization rules.
     *
     * <p>Returned as a {@link List} so that the order and multiplicity the principal source produced are
     * preserved: how they are interpreted is the evaluator's decision, not this contract's. The plugin's
     * default evaluator matches them by exact name as an unordered set — holding {@code ADMIN} satisfies a
     * requirement for {@code ADMIN} and nothing else, with no entitlement outranking another — but a custom
     * evaluator is free to read significance into the ordering.
     *
     * @return the client's entitlements, never {@code null} for authenticated requests (may be empty)
     */
    List<String> authenticatedClientEntitlements();

    /**
     * Returns the set of OAuth2/custom scopes granted to the authenticated client.
     * Scopes are used by the Access Control plugin to enforce fine-grained authorization rules.
     *
     * @return set of scope strings, never {@code null} (may be empty)
     */
    Set<String> authenticatedClientScopes();

    /**
     * Returns custom attributes carried by the authenticated principal.
     * A JWT token usually carries additional claims (e.g. user email, expiration date, etc) that can be
     * parsed here and read back from the currently logged in principal via
     * {@link AuthenticatedPrincipalContextHolder}, or used for ABAC evaluations in the
     * Access Control plugin. Can be missing for server-to-server auth flows.
     *
     * @return attributes keyed by name, never {@code null} (may be empty)
     */
    Map<String, Object> attributes();

}
