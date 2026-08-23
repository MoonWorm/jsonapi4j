package pro.api4.jsonapi4j.plugin.ac.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * One clause of a scopes requirement: a set of OAuth2 scope names and how the scopes granted to the caller
 * are matched against them.
 * <p>
 * Scopes are authority delegated by the resource owner, carried on the access token. Matching is by exact
 * name — a token granting {@code users.read} does not satisfy a requirement for {@code users.write}.
 * <p>
 * Clauses are listed in {@link AccessControlScopes#value()} and combined by that annotation's
 * {@link AccessControlScopes#mode()}.
 */
@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface ScopesGroup {

    /**
     * Returns the scope names this clause is about. Must not be empty — omit the clause entirely to express
     * "no requirement".
     *
     * @return scope names
     */
    String[] value();

    /**
     * Returns how {@link #value()} is matched against the scopes granted to the caller.
     *
     * @return matching mode, {@link Mode#ALL_OF} by default — a scope requirement normally asks for every
     * scope it lists
     */
    Mode mode() default Mode.ALL_OF;

    /**
     * How a set of scope names is matched against the scopes a caller was granted.
     * <p>
     * These are quantifiers over a set rather than binary operators, which keeps the meaning unambiguous
     * when a set holds one element and makes negation a member of the same family instead of a separate flag.
     */
    enum Mode {

        /**
         * The caller must have been granted every listed scope.
         */
        ALL_OF,

        /**
         * The caller must have been granted at least one of the listed scopes.
         */
        ANY_OF,

        /**
         * The caller must have been granted none of the listed scopes.
         */
        NONE_OF
    }

}
