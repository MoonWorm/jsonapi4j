package pro.api4.jsonapi4j.domain.plugin.oas.annotation;

import pro.api4.jsonapi4j.domain.Resource;
import pro.api4.jsonapi4j.domain.plugin.oas.model.NoLinkageMeta;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface OasRelationshipInfo {

    Class<?> resourceLinkageMetaType() default NoLinkageMeta.class;

    Class<? extends Resource<?>>[] relationshipTypes() default {};

}
