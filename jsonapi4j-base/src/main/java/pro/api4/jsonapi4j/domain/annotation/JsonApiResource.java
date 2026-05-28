package pro.api4.jsonapi4j.domain.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks an interface as a JSON:API resource definition.
 * <p>
 * Apply this annotation at the type level on any class or interface that implements
 * {@link pro.api4.jsonapi4j.domain.Resource}. The framework uses it at startup to
 * discover and register the resource in the {@link pro.api4.jsonapi4j.domain.DomainRegistry},
 * and at runtime to resolve the JSON:API {@code "type"} member for every resource object
 * produced by this implementation.
 * <p>
 * Example:
 * <pre>{@code
 * @JsonApiResource(resourceType = "users")
 * public class UserResource implements Resource<UserEntity> { ... }
 * }</pre>
 *
 * @see pro.api4.jsonapi4j.domain.Resource
 * @see pro.api4.jsonapi4j.operation.annotation.JsonApiResourceOperation
 */
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
