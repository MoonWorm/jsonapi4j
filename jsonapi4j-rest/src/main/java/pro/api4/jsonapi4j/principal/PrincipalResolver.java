package pro.api4.jsonapi4j.principal;

import jakarta.servlet.ServletRequest;

/**
 * Builds the {@link Principal} for the current request.
 * <p>
 * JsonApi4j never authenticates anyone — verifying credentials is the job of the surrounding web framework
 * or API gateway. A resolver answers only the narrower question: given a request that has already passed
 * authentication, who is the caller and what are they allowed to do?
 * <p>
 * Called once per request by the principal-resolving servlet filter, which stores the result on the request
 * thread and clears it when the request completes.
 * <p>
 * A single instance serves every request, so implementations must be thread-safe — resolve everything from
 * the {@link ServletRequest} argument (or a request-scoped proxy) rather than caching state on the resolver.
 * <p>
 * {@link DefaultPrincipalResolver} is active unless an application registers its own; see
 * <a href="https://api4.pro/principal-resolution/">Principal Resolution</a> for the shipped implementations
 * and how to register one.
 *
 * @see Principal
 * @see AuthenticatedPrincipalContextHolder
 */
public interface PrincipalResolver {

    /**
     * Resolves the caller of the current request.
     * <p>
     * Returning {@code null} marks the request as anonymous, as does returning a principal whose user id is
     * {@code null}. Any value a resolver cannot determine is left {@code null} or empty on the returned
     * principal rather than guessed — the Access Control plugin treats absent and empty alike, so an
     * unresolvable value fails every requirement that asks for it instead of granting access by default.
     *
     * @param servletRequest the current request
     * @return the caller, or {@code null} for an anonymous request
     */
    Principal resolvePrincipal(ServletRequest servletRequest);

}
