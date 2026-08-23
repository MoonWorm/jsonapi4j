package pro.api4.jsonapi4j.plugin.ac.annotation;

import pro.api4.jsonapi4j.plugin.ac.entitlement.EntitlementsPolicy;
import pro.api4.jsonapi4j.plugin.ac.entitlement.NoOpEntitlementsPolicy;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the entitlements a caller must hold to reach the annotated field or type. A caller that fails the
 * requirement is denied, and the marked field or type is filtered out of the JSON response.
 * <p>
 * A requirement is a list of {@link EntitlementsGroup} clauses combined by {@link #mode()}, giving two levels
 * of structure — each clause quantifies over entitlement names, and this annotation quantifies over clauses:
 * <pre>{@code
 * @AccessControl(entitlements = @AccessControlEntitlements(
 *         mode = Mode.ANY_OF,
 *         description = "internal support staff, or a partner integration",
 *         value = {
 *                 @EntitlementsGroup(mode = ALL_OF, value = {ADMIN, SUPPORT}),
 *                 @EntitlementsGroup(mode = ALL_OF, value = {PARTNER, PUBLIC})
 *         }))
 * }</pre>
 * which reads as {@code (ADMIN and SUPPORT) or (PARTNER and PUBLIC)}.
 * <p>
 * Two levels is the limit — Java annotations cannot nest arbitrarily. For anything deeper, or for rules that
 * count matches or consider more than entitlements, declare a {@link #policy()} instead: it is ordinary code
 * with no structural limit. A requirement declares either clauses or a policy, never both.
 *
 * @see EntitlementsGroup
 * @see EntitlementsPolicy
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControlEntitlements {

    /**
     * Returns the clauses making up this requirement, combined by {@link #mode()}. Empty when a
     * {@link #policy()} is declared instead.
     *
     * @return the requirement's clauses
     */
    EntitlementsGroup[] value() default {};

    /**
     * Returns how {@link #value()} clauses are combined. Ignored when a {@link #policy()} is declared.
     *
     * @return combining mode, {@link Mode#ALL_OF} by default — every clause must be satisfied
     */
    Mode mode() default Mode.ALL_OF;

    /**
     * Returns a human-readable reason for this requirement, quoted when access is denied so that a denial
     * explains itself rather than only naming entitlements. Optional.
     *
     * @return the requirement's description, empty when not set
     */
    String description() default "";

    /**
     * Returns a policy deciding this requirement in code, for rules the two-level declarative form cannot
     * express. Replaces {@link #value()} rather than adding to it; declaring both is a misconfiguration.
     *
     * @return the policy type, {@link NoOpEntitlementsPolicy} when no policy is declared
     */
    Class<? extends EntitlementsPolicy> policy() default NoOpEntitlementsPolicy.class;

    /**
     * How a set of clauses is combined into one decision. Mirrors {@link EntitlementsGroup.Mode}, one level up.
     */
    enum Mode {

        /**
         * Every clause must be satisfied.
         */
        ALL_OF,

        /**
         * At least one clause must be satisfied.
         */
        ANY_OF,

        /**
         * No clause may be satisfied.
         */
        NONE_OF
    }

}
