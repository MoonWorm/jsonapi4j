package pro.api4.jsonapi4j.ac.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControlScopes {

    String NOT_SET = "__NOT_SET__";

    /**
     * Defines which scopes are required in order to access marked field or entire type.
     * If client doesn't have the required scopes the marked field or type will be filtered out from
     * the resulting JSON response.
     * <p>
     * Defaults to an array of {@link #NOT_SET} element which tells framework to do not run evaluations for this requirement.
     * <p>
     * Mutual exclusive with {@link #requiredScopesExpression()}. Ignored if scopes expression has been specified.
     * <p>
     * Applicable on any JSON:API document level: JSON:API Doc (Controller), JSON:API Resource (Type & Fields), JSON:API Relationships (Type & Fields).
     *
     * @return array of required scopes
     */
    String[] requiredScopes() default { NOT_SET };

    /**
     * Defines which scopes are required in order to access marked field or entire type.
     * If client doesn't have the required scopes the marked field or type will be filtered out from
     * the resulting JSON response.
     * <p>
     * Defaults to {@link #NOT_SET} which tells framework to do not run evaluations for this requirement.
     * <p>
     * Mutual exclusive with {@link #requiredScopes()}. This one is applied when both are specified.
     * <p>
     * Applicable on eny JSON:API document level: JSON:API Doc (Controller), JSON:API Resource (Type & Fields), JSON:API Relationships (Type & Fields).
     *
     * @return scopes SpEL expression
     */
    String requiredScopesExpression() default NOT_SET;

}
