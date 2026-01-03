package pro.api4.jsonapi4j.operation.annotation;

import pro.api4.jsonapi4j.domain.Resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiResourceOperation {

    Class<? extends Resource<?>> resource();

}
