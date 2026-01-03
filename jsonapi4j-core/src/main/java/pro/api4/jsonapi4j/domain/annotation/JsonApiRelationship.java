package pro.api4.jsonapi4j.domain.annotation;

import pro.api4.jsonapi4j.domain.Resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiRelationship {

    /**
     * @return a String that represents the name of the relationship e.g.
     * "userProperties", "userCitizenships", etc.
     */
    String relationshipName();

    Class<? extends Resource<?>> parentResource();

}
