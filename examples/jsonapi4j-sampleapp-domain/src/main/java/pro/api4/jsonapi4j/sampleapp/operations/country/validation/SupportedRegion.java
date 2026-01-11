package pro.api4.jsonapi4j.sampleapp.operations.country.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.TYPE_USE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Constraint(validatedBy = SupportedRegionValidator.class)
public @interface SupportedRegion {

    String message() default "Unknown region";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};

}
