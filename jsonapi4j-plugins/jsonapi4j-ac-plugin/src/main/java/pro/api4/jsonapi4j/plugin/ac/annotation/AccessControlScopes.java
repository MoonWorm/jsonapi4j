package pro.api4.jsonapi4j.plugin.ac.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Declares the OAuth2 scopes a caller must have been granted to reach the annotated field or type. A caller
 * that fails the requirement is denied, and the marked field or type is filtered out of the JSON response.
 * <p>
 * A requirement is a list of {@link ScopesGroup} clauses combined by {@link #mode()}, giving two levels of
 * structure — each clause quantifies over scope names, and this annotation quantifies over clauses:
 * <pre>{@code
 * @AccessControl(scopes = @AccessControlScopes(
 *         description = "read access to profiles, or full admin access",
 *         mode = Mode.ANY_OF,
 *         value = {
 *                 @ScopesGroup({"users.read", "profiles.read"}),
 *                 @ScopesGroup("admin.full")
 *         }))
 * }</pre>
 * which reads as {@code (users.read and profiles.read) or admin.full}.
 * <p>
 * The simple case stays short — one clause, defaulting to {@code ALL_OF}:
 * <pre>{@code
 * @AccessControl(scopes = @AccessControlScopes(@ScopesGroup("users.sensitive.read")))
 * }</pre>
 * <p>
 * Two levels is the limit — Java annotations cannot nest arbitrarily. For anything deeper, or for rules that
 * weigh scopes together with the operation, the resource or the caller's attributes, declare an
 * {@link AccessControlPolicy} instead: it is ordinary code with no structural limit, and it can read the
 * granted scopes from {@code context.principal().authenticatedClientScopes()}.
 *
 * @see ScopesGroup
 * @see pro.api4.jsonapi4j.plugin.ac.policy.AccessPolicy
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControlScopes {

    /**
     * Returns the clauses making up this requirement, combined by {@link #mode()}. Must not be empty — omit
     * the annotation entirely to declare no scope requirement.
     *
     * @return the requirement's clauses
     */
    ScopesGroup[] value() default {};

    /**
     * Returns how {@link #value()} clauses are combined.
     *
     * @return combining mode, {@link Mode#ALL_OF} by default — every clause must be satisfied
     */
    Mode mode() default Mode.ALL_OF;

    /**
     * Returns a human-readable reason for this requirement, quoted when access is denied so that a denial
     * explains itself rather than only naming scopes. Optional.
     *
     * @return the requirement's description, empty when not set
     */
    String description() default "";

    /**
     * How a set of clauses is combined into one decision. Mirrors {@link ScopesGroup.Mode}, one level up.
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
