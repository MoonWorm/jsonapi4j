package pro.api4.jsonapi4j.plugin.oas.domain.annotation;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.plugin.oas.domain.model.NoLinkageMeta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Must be placed on the class that implements {@link pro.api4.jsonapi4j.domain.Relationship} interface.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OasRelationshipInfo {

    Class<?> resourceLinkageMetaType() default NoLinkageMeta.class;

    Class<? extends Resource<?>>[] relationshipTypes() default {};

}
