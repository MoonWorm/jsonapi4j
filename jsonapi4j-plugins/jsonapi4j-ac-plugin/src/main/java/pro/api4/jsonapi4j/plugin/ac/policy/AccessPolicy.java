package pro.api4.jsonapi4j.plugin.ac.policy;

import pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext;

/**
 * Decides an access control requirement in code, for rules the declarative forms cannot express.
 * <p>
 * The declarative requirements each have a fixed shape: {@code authenticated} is a flag, {@code entitlements}
 * is two levels of name matching, {@code scopes} is a scope expression, {@code ownership} is an id comparison.
 * A policy has no shape at all — it receives the whole {@link AccessControlContext} and returns a decision, so
 * it can nest freely, count matches, read the caller's {@code attributes()}, branch on the operation being
 * performed, or compare the caller against the resource being returned.
 * <p>
 * Declare one with
 * {@code @AccessControl(policy = @AccessControlPolicy(MyPolicy.class))}. It is combined with the other
 * requirements the same way they are combined with each other: every declared requirement must pass.
 * <p>
 * Implementations must be stateless and thread-safe — a single instance is created when the access control
 * model is built and then shared by every request that reaches the annotated element. They must expose a
 * public no-argument constructor.
 *
 * @see AccessControlContext
 * @see pro.api4.jsonapi4j.plugin.ac.ownership.OwnerIdExtractor
 */
public interface AccessPolicy {

    /**
     * Decides whether the current evaluation is allowed.
     *
     * @param context everything the decision may be based on, never {@code null}
     * @return {@code true} to allow, {@code false} to deny
     */
    boolean isSatisfiedBy(AccessControlContext context);

}
