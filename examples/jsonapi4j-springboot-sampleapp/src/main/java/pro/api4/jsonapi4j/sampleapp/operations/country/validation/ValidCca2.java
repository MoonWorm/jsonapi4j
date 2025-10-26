package pro.api4.jsonapi4j.sampleapp.operations.country.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = Cca2Validator.class)
public @interface ValidCca2 {

    String message() default "Not valid CCA2 country code";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
