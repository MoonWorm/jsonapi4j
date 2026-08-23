package pro.api4.jsonapi4j.plugin.ac.entitlement;

import java.util.List;

/**
 * Decides an entitlement requirement in code, for policies the declarative form cannot express.
 * <p>
 * {@code @AccessControlEntitlements} covers two levels — groups of entitlements combined by one operator —
 * because Java annotations cannot nest arbitrarily. A policy has no such limit: it can nest freely, count
 * matches, or read anything else it needs from the request context. Point an annotation at one with
 * {@code @AccessControl(entitlements = @AccessControlEntitlements(policy = MyPolicy.class))}.
 * <p>
 * Implementations must be stateless and thread-safe: a single instance is created when the access control
 * model is built and then shared by every request that reaches the annotated element. They must expose a
 * public no-argument constructor.
 * <p>
 * A policy replaces the declarative groups rather than adding to them — declaring both is rejected as a
 * misconfiguration, so that one annotation always has exactly one source of truth.
 *
 * @see pro.api4.jsonapi4j.plugin.ac.ownership.OwnerIdExtractor
 */
public interface EntitlementsPolicy {

    /**
     * Decides whether the entitlements the caller holds satisfy this policy.
     * <p>
     * The list is never {@code null} — a caller carrying none is passed an empty list — and preserves the
     * order and multiplicity the {@code PrincipalResolver} produced, so a policy may read significance into
     * either if it wants to.
     *
     * @param entitlements the entitlements held by the authenticated caller, never {@code null}
     * @return {@code true} to allow, {@code false} to deny
     */
    boolean isSatisfiedBy(List<String> entitlements);

}
