package pro.api4.jsonapi4j.ac.annotation;

import pro.api4.jsonapi4j.ac.tier.TierNoAccess;
import pro.api4.jsonapi4j.ac.tier.TierPublic;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControlAccessTier {

    /**
     * Defines which access tier is required in order to access marked field or entire type.
     * If client doesn't have the required access tier the marked field or type will be filtered out from
     * the resulting JSON response.
     * <p>
     * Defaults to {@link TierPublic#PUBLIC_TIER} which has the lowest weight except for {@link TierNoAccess#NO_ACCESS_TIER}.
     *
     * @return required access tier
     */
    String value() default TierPublic.PUBLIC_TIER;

}
