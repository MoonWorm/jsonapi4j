package pro.api4.jsonapi4j.plugin.ac.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AccessControl {

    Authenticated authenticated() default Authenticated.NOT_SET;

    AccessControlScopes scopes() default @AccessControlScopes();

    AccessControlAccessTier tier() default @AccessControlAccessTier();

    AccessControlOwnership ownership() default @AccessControlOwnership();


}
