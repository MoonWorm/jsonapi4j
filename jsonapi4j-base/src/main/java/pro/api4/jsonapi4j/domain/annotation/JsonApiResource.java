package pro.api4.jsonapi4j.domain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiResource {

    /**
     * The corresponding resource's type ("type" member).
     * <p>
     * Must be unique across all domains.
     *
     * @return a String that represents the current resource type.
     */
    String resourceType();

}
