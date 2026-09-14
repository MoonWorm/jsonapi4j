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

    /**
     * Describes this resource's identifier wherever it appears - the {@code {id}} path parameter of every operation
     * acting on one instance, and the {@code id} member of the resource schema. Left empty, a generic wording is
     * used.
     * <p>
     * Request bodies keep their own wording, which says whether the id must match the path or may be omitted, since
     * that is about the operation rather than the resource.
     */
    String resourceIdDescription() default "";

    /**
     * An example identifier for this resource, applied wherever its id appears. Left empty, a generic one is used.
     */
    String resourceIdExample() default "";

    /**
     * Marks every operation on this resource deprecated. Deprecation only ever widens - an operation may deprecate
     * itself through {@link pro.api4.jsonapi4j.plugin.oas.operation.annotation.OasOperationInfo#deprecated()}, but
     * none can opt out of a resource that is going away.
     */
    boolean deprecated() default false;

}
