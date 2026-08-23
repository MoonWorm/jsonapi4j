package pro.api4.jsonapi4j.plugin.ac.annotation;

import pro.api4.jsonapi4j.plugin.ac.policy.AccessPolicy;
import pro.api4.jsonapi4j.plugin.ac.policy.NoOpAccessPolicy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares that access to the annotated field or type is decided by an {@link AccessPolicy} — code, rather
 * than one of the declarative requirement forms.
 * <p>
 * Use it for rules the other requirements cannot express: anything that reads the caller's
 * {@code attributes()}, branches on the operation being performed, compares the caller against the resource
 * being returned, or nests more deeply than {@code @AccessControlEntitlements} allows.
 * <pre>{@code
 * @AccessControl(policy = @AccessControlPolicy(
 *         value = SameTenantPolicy.class,
 *         description = "caller may only see resources of their own tenant"))
 * }</pre>
 * <p>
 * A policy is combined with the other requirements exactly as they are combined with each other: every
 * declared requirement must pass. It is evaluated last, after the declarative ones.
 *
 * @see AccessPolicy
 * @see pro.api4.jsonapi4j.plugin.ac.context.AccessControlContext
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControlPolicy {

    /**
     * Returns the policy deciding this requirement.
     *
     * @return the policy type, {@link NoOpAccessPolicy} when no policy is declared
     */
    Class<? extends AccessPolicy> value() default NoOpAccessPolicy.class;

    /**
     * Returns a human-readable reason for this requirement, quoted when access is denied so that a denial
     * explains itself rather than only naming a class. Optional.
     *
     * @return the requirement's description, empty when not set
     */
    String description() default "";

}
