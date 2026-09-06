package pro.api4.jsonapi4j.plugin.oas.operation.annotation;

import pro.api4.jsonapi4j.plugin.oas.operation.model.In;
import pro.api4.jsonapi4j.plugin.oas.operation.model.NotApplicable;
import pro.api4.jsonapi4j.plugin.oas.operation.model.PaginationStyle;
import pro.api4.jsonapi4j.plugin.oas.operation.model.Type;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Must be placed on the class that implements {@link pro.api4.jsonapi4j.operation.Operation} interface.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OasOperationInfo {

    /**
     * Overrides the operation's generated summary. Left empty, the plugin derives one from the operation type and
     * the resource name.
     */
    String summary() default "";

    /**
     * Overrides the operation's generated description. Left empty, the plugin derives one from the operation type
     * and the resource name.
     */
    String description() default "";

    /**
     * Attribute paths this operation can sort by. Declaring them publishes the {@code sort} query parameter and
     * enumerates what it accepts; leaving them empty means the operation does not sort, and no parameter is
     * published — the framework parses {@code sort} for every request, but only the operation can honour it.
     */
    String[] sortableFields() default {};

    /**
     * Pagination styles this operation honours. Defaults to {@link PaginationStyle#CURSOR}, which is what the
     * framework has always documented; add {@link PaginationStyle#LIMIT_OFFSET} when the operation reads
     * {@code page[limit]} and {@code page[offset]}. Ignored for operations that are not paginated.
     */
    PaginationStyle[] pagination() default {PaginationStyle.CURSOR};

    SecurityConfig securityConfig() default @SecurityConfig;

    Parameter[] parameters() default {};

    Class<?> payloadType() default NotApplicable.class;

    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface SecurityConfig {
        boolean clientCredentialsSupported() default false;
        boolean pkceSupported() default false;
        String[] requiredScopes() default {};
    }

    @Target({})
    @Retention(RetentionPolicy.RUNTIME)
    @interface Parameter {
        String name();
        In in() default In.QUERY;
        String description() default  "";
        String example() default "";
        boolean required() default true;
        boolean array() default false;
        Type type() default Type.STRING;
    }

}
