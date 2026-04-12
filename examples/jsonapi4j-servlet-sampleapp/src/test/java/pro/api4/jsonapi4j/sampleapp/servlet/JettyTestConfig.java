package pro.api4.jsonapi4j.sampleapp.servlet;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the JsonApi4j YAML config file to use when starting the embedded Jetty server.
 * The value is a classpath resource path, e.g. {@code "/jsonapi4j-domainTest.yaml"}.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface JettyTestConfig {

    String value();

}
