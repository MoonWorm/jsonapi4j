package pro.api4.jsonapi4j.domain.plugin.oas.model;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OasResourceInfo {

    String resourceNameSingle() default "";

    String resourceNamePlural() default  "";

    Class<?> attributes() default NoAttributes.class;

    class NoAttributes {

    }

}
