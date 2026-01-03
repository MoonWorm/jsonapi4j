package pro.api4.jsonapi4j.operation.plugin.oas.model;

import lombok.Getter;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OasOperationInfo {

    SecurityConfig securityConfig() default @SecurityConfig;

    Parameter[] parameters() default  {};

    Class<?> payloadType() default NotApplicable.class;

    @Getter
    enum In {
        QUERY("query"), PATH("path"), HEADER("header");

        private final String name;

        In(String name) {
            this.name = name;
        }

    }

    @Getter
    enum Type {
        STRING("string"), NUMBER("number"), INTEGER("integer"), BOOLEAN("boolean");

        private String type;

        Type(String type) {
            this.type = type;
        }

    }

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

    class NotApplicable {

    }

}
