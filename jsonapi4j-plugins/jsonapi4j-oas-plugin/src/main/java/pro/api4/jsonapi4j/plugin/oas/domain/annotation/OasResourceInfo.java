package pro.api4.jsonapi4j.plugin.oas.domain.annotation;

import pro.api4.jsonapi4j.plugin.oas.domain.model.NoAttributes;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Must be placed on the class that implements {@link pro.api4.jsonapi4j.domain.Resource} interface.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OasResourceInfo {

    /**
     * Overrides the singular form the plugin otherwise guesses from the resource type, used in the ids of operations
     * acting on one resource and in the generated prose. The plural form is the resource type itself and needs no
     * override.
     */
    String resourceNameSingle() default "";

    Class<?> attributes() default NoAttributes.class;

}
