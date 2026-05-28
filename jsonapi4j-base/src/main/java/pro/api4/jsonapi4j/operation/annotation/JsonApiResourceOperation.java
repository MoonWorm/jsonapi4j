package pro.api4.jsonapi4j.operation.annotation;

import pro.api4.jsonapi4j.domain.Resource;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Associates a resource operation implementation with its JSON:API resource definition.
 * <p>
 * Apply this annotation at the type level on any class that implements one or more resource
 * operation interfaces ({@link pro.api4.jsonapi4j.operation.ReadResourceByIdOperation},
 * {@link pro.api4.jsonapi4j.operation.ReadMultipleResourcesOperation},
 * {@link pro.api4.jsonapi4j.operation.CreateResourceOperation},
 * {@link pro.api4.jsonapi4j.operation.UpdateResourceOperation},
 * {@link pro.api4.jsonapi4j.operation.DeleteResourceOperation}, or the composite
 * {@link pro.api4.jsonapi4j.operation.ResourceOperations}).
 * The framework uses this annotation at startup to register the operation
 * in the {@link pro.api4.jsonapi4j.operation.OperationsRegistry} under the correct resource type.
 * <p>
 * Example:
 * <pre>{@code
 * @JsonApiResourceOperation(resource = UserResource.class)
 * public class UserOperations implements ResourceOperations<UserEntity> { ... }
 * }</pre>
 *
 * @see pro.api4.jsonapi4j.operation.ResourceOperations
 * @see pro.api4.jsonapi4j.domain.annotation.JsonApiResource
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface JsonApiResourceOperation {

    /**
     * The {@link Resource} implementation this operation belongs to.
     * Used by the framework to look up the resource type during operation registration.
     *
     * @return the resource class annotated with {@link JsonApiResource}
     */
    Class<? extends Resource<?>> resource();

}
