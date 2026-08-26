package pro.api4.jsonapi4j.plugin.ac.annotation;

import pro.api4.jsonapi4j.principal.entitlement.DefaultEntitlements;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * One clause of an entitlement requirement: a set of entitlement names and how the caller's entitlements are
 * matched against them.
 * <p>
 * Entitlements are unordered labels, not privilege levels — holding one grants nothing beyond that
 * entitlement. The framework ships {@link DefaultEntitlements#PARTNER}, {@link DefaultEntitlements#ADMIN}
 * and {@link DefaultEntitlements#ROOT_ADMIN} as ready-made constants, but any string a
 * {@code PrincipalResolver} can produce works.
 * <p>
 * Groups are listed in {@link AccessControlEntitlements#value()} and combined by that annotation's
 * {@link AccessControlEntitlements#mode()}.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface EntitlementsGroup {

    /**
     * Returns the entitlement names this clause is about. Must not be empty — omit the clause entirely to
     * express "no requirement".
     *
     * @return entitlement names
     */
    String[] value();

    /**
     * Returns how {@link #value()} is matched against the entitlements the caller holds.
     *
     * @return matching mode, {@link Mode#ANY_OF} by default
     */
    Mode mode() default Mode.ANY_OF;

    /**
     * How a set of names is matched against the entitlements a caller holds.
     * <p>
     * These are quantifiers over a set rather than binary operators, which is why they read {@code ALL_OF} /
     * {@code ANY_OF} / {@code NONE_OF} rather than {@code AND} / {@code OR}: it keeps the meaning unambiguous
     * when a set holds one element, and makes negation a member of the same family instead of a separate flag.
     */
    enum Mode {

        /**
         * The caller must hold every listed entitlement.
         */
        ALL_OF,

        /**
         * The caller must hold at least one of the listed entitlements.
         */
        ANY_OF,

        /**
         * The caller must hold none of the listed entitlements.
         */
        NONE_OF
    }

}
