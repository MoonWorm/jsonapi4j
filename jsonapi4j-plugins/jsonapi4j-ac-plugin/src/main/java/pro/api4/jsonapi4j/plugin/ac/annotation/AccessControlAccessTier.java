package pro.api4.jsonapi4j.plugin.ac.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControlAccessTier {

    String NOT_SET = "__NOT_SET__";

    /**
     * Defines which access tier is required in order to access marked field or entire type.
     * If client doesn't have the required access tier the marked field or type will be filtered out from
     * the resulting JSON response.
     * <p>
     * Defaults to {@link #NOT_SET} which tells the framework no access tier evaluations to execute.
     * <p>
     * Other available by default tiers (from lowest to highest):
     * <ol>
     *     <li>{@link TierRootAdmin#ROOT_ADMIN_ACCESS_TIER}</li>
     *     <li>{@link TierAdmin#ADMIN_ACCESS_TIER}</li>
     *     <li>{@link TierPartner#PARTNER_ACCESS_TIER}</li>
     *     <li>{@link TierPublic#PUBLIC_TIER}</li>
     *     <li>{@link TierNoAccess#NO_ACCESS_TIER}</li>
     * </ol>
     *
     * @return required access tier
     */
    String value() default NOT_SET;

}
